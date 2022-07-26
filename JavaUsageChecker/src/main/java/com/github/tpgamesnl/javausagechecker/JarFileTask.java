package com.github.tpgamesnl.javausagechecker;

import com.github.tpgamesnl.javausagechecker.worker.Task;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Queue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarFileTask implements Task {

    private final JavaUsageChecker javaUsageChecker;
    private final Queue<JarEntryTask> jarEntryTasks;

    private final File file;

    public JarFileTask(JavaUsageChecker javaUsageChecker, Queue<JarEntryTask> jarEntryTasks, File file) {
        this.javaUsageChecker = javaUsageChecker;
        this.jarEntryTasks = jarEntryTasks;
        this.file = file;
    }

    @Override
    public void perform() {
        JarFile jarFile;
        try {
            jarFile = new JarFile(file);

            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();

                if (jarEntry.isDirectory() || !jarEntry.getName().endsWith(".class")) {
                    continue;
                }

                this.jarEntryTasks.add(new JarEntryTask(javaUsageChecker, jarFile, jarEntry));
            }
        } catch (IOException e) {
            new RuntimeException("Error creating JarFile from " + file.getName(), e).printStackTrace();
        }

        javaUsageChecker.getStateTracker().incrementJarOpenedCount();
    }

}
