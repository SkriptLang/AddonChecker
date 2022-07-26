package com.github.tpgamesnl.javausagechecker;

import com.github.tpgamesnl.javausagechecker.worker.Task;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarEntryTask implements Task {

    private final JavaUsageChecker javaUsageChecker;
    private final JarFile jarFile;
    private final JarEntry jarEntry;

    public JarEntryTask(JavaUsageChecker javaUsageChecker, JarFile jarFile, JarEntry jarEntry) {
        this.javaUsageChecker = javaUsageChecker;
        this.jarFile = jarFile;
        this.jarEntry = jarEntry;
    }

    @Override
    public void perform() {
        try {
            InputStream inputStream = jarFile.getInputStream(jarEntry);

            String className = JavaUsageChecker.formatClassName(jarEntry.getName());

            ClassReader classReader = new ClassReader(inputStream);

            ClassVisitor usageCheckerCV = new UsageCheckerCV(null, javaUsageChecker, jarFile.getName(), className);
            classReader.accept(usageCheckerCV, 0);
        } catch (IOException e) {
            new RuntimeException(e).printStackTrace();
        }

        javaUsageChecker.getStateTracker().incrementClassesCheckedCount();
    }
}
