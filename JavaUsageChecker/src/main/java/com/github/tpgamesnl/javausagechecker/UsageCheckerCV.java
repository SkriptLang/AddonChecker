package com.github.tpgamesnl.javausagechecker;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class UsageCheckerCV extends ClassVisitor {

    private final JavaUsageChecker javaUsageChecker;
    private final String jarFilePath;
    private final String filePath;

    public UsageCheckerCV(ClassVisitor classVisitor, JavaUsageChecker javaUsageChecker, String jarFilePath, String filePath) {
        super(Opcodes.ASM9, classVisitor);
        this.javaUsageChecker = javaUsageChecker;
        this.jarFilePath = jarFilePath;
        this.filePath = filePath;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv =  super.visitMethod(access, name, descriptor, signature, exceptions);
        return new MethodCheckerMV(mv, name, descriptor);
    }

    public class MethodCheckerMV extends MethodVisitor {
        private final String methodName;
        private final String methodDescriptor;

        public MethodCheckerMV(MethodVisitor mv, String methodName, String methodDescriptor) {
            super(Opcodes.ASM9, mv);
            this.methodName = methodName;
            this.methodDescriptor = methodDescriptor;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            javaUsageChecker.reportMethodAccess(jarFilePath, filePath, methodName, methodDescriptor, opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            super.visitFieldInsn(opcode, owner, name, descriptor);
            javaUsageChecker.reportFieldAccess(jarFilePath, filePath, methodName, methodDescriptor, opcode, owner, name, descriptor);
        }
    }

}
