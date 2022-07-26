package com.github.tpgamesnl.javausagechecker;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class UsageCheckerCV extends ClassVisitor {

    protected final JavaUsageChecker javaUsageChecker;
    protected final ClassLocation classLocation;

    public UsageCheckerCV(ClassVisitor classVisitor, JavaUsageChecker javaUsageChecker, String jarFileName, String className) {
        super(Opcodes.ASM9, classVisitor);
        this.javaUsageChecker = javaUsageChecker;
        this.classLocation = new ClassLocation(jarFileName, className);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);

        if (superName != null)
            javaUsageChecker.reportClassUsage(classLocation.extendsClass(), superName);
        for (String interfaceName : interfaces) {
            javaUsageChecker.reportClassUsage(classLocation.implementsClass(), interfaceName);
        }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        return new MethodCheckerMV(mv, name, descriptor);
    }

    public class MethodCheckerMV extends MethodVisitor {
        private final ClassLocation.Method method;
        private int lastLineNumber = -1;

        public MethodCheckerMV(MethodVisitor mv, String methodName, String methodDescriptor) {
            super(Opcodes.ASM9, mv);
            this.method = classLocation.inMethod(methodName, methodDescriptor);

            Type returnType = Type.getReturnType(methodDescriptor);
            javaUsageChecker.reportClassUsage(method.inReturnType(), returnType.getClassName());

            Type[] argumentTypes = Type.getArgumentTypes(methodDescriptor);
            for (int i = 0; i < argumentTypes.length; i++) {
                javaUsageChecker.reportClassUsage(method.inParameter(i), argumentTypes[i].getClassName());
            }
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            super.visitLineNumber(line, start);
            lastLineNumber = line;
        }

        @Override
        public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            return super.visitParameterAnnotation(parameter, descriptor, visible);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);

            ClassLocation.Method.Code code = method.inCode(lastLineNumber);

            javaUsageChecker.reportMethodAccess(code, opcode, owner, name, descriptor, isInterface);

            javaUsageChecker.reportClassUsage(code, owner);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            super.visitFieldInsn(opcode, owner, name, descriptor);

            ClassLocation.Method.Code code = method.inCode(lastLineNumber);

            javaUsageChecker.reportFieldAccess(code, opcode, owner, name, descriptor);

            javaUsageChecker.reportClassUsage(code, owner);
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            super.visitTypeInsn(opcode, type);

            ClassLocation.Method.Code code = method.inCode(lastLineNumber);
            javaUsageChecker.reportClassUsage(code, type);
        }

        @Override
        public void visitLdcInsn(Object value) {
            super.visitLdcInsn(value);

            if (value instanceof Type) {
                javaUsageChecker.reportClassUsage(method.inCode(lastLineNumber), ((Type) value).getClassName());
            }
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        Type type = Type.getType(descriptor);
        javaUsageChecker.reportClassUsage(classLocation.inField(name), type.getClassName());

        return super.visitField(access, name, descriptor, signature, value);
    }
}
