package com.github.tpgamesnl.javausagechecker;

import com.github.tpgamesnl.javausagechecker.query.Query;
import com.github.tpgamesnl.javausagechecker.worker.Task;
import com.github.tpgamesnl.javausagechecker.worker.WorkerCollection;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class JavaUsageChecker {

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {

        private final List<File> files = new ArrayList<>();
        private final List<Query> queries = new ArrayList<>();
        private int threadCount = 1;

        private Builder() { }

        public Builder uses(Query query) {
            this.queries.add(query);
            return this;
        }

        public Builder scans(File... files) {
            this.files.addAll(Arrays.asList(files));
            return this;
        }

        public Builder threadCount(int threadCount) {
            this.threadCount = threadCount;
            return this;
        }

        public JavaUsageChecker create() {
            return new JavaUsageChecker(files, queries, threadCount);
        }

        public List<File> getFiles() {
            return files;
        }

        public List<Query> getQueries() {
            return queries;
        }

        public int getThreadCount() {
            return threadCount;
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "files=" + files +
                    ", queries=" + queries +
                    ", threadCount=" + threadCount +
                    '}';
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public class JarFileTask implements Task {

        private final Queue<JarEntryTask> jarEntryTasks;

        private final File file;

        public JarFileTask(Queue<JarEntryTask> jarEntryTasks, File file) {
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

                    this.jarEntryTasks.add(new JarEntryTask(jarFile, jarEntry));
                }
            } catch (IOException e) {
                throw new RuntimeException("Error creating JarFile from " + file.getName(), e);
            }
        }

    }

    public class JarEntryTask implements Task {

        private final JarFile jarFile;
        private final JarEntry jarEntry;

        public JarEntryTask(JarFile jarFile, JarEntry jarEntry) {
            this.jarFile = jarFile;
            this.jarEntry = jarEntry;
        }

        @Override
        public void perform() {
            try {
                InputStream inputStream = jarFile.getInputStream(jarEntry);
                if (!jarEntry.getName().endsWith(".class")) {
                    return;
                }

                String className = jarEntry.getName().replace('/', '.');

                ClassReader classReader = new ClassReader(inputStream);

                ClassVisitor usageCheckerCV = new UsageCheckerCV(null, JavaUsageChecker.this, jarFile.getName(), className);
                classReader.accept(usageCheckerCV, 0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final List<File> files;
    private final List<Query> queries;
    private final List<Report> reports;

    private final int workerCount;

    private WorkerCollection<JarEntryTask> jarEntryTaskWorkerCollection;

    public JavaUsageChecker(List<File> files, List<Query> queries, int workerCount) {
        this.files = files;
        this.queries = queries;
        this.reports = new ArrayList<>();
        this.workerCount = workerCount;
    }

    public JavaUsageChecker start() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        List<File> actualFiles = new ArrayList<>();
        for (File file : this.files) {
            actualFiles.addAll(expandFile(file));
        }

        Queue<JarFileTask> jarFileTasks = new LinkedList<>();
        Queue<JarEntryTask> jarEntryTasks = new LinkedList<>();
        for (File file : actualFiles) {
            jarFileTasks.add(new JarFileTask(jarEntryTasks, file));
        }

        // TODO replace message prints with an interface like UpdateListener
        System.out.println();
        System.out.println("Starting to open " + jarFileTasks.size() + " JarFiles");
        System.out.println();

        WorkerCollection.create(workerCount, "JarFile-opener-", jarFileTasks)
                .start()
                .join();

        System.out.println();
        System.out.println("JarFiles opened, starting to check " + jarEntryTasks.size() + " JarEntries");
        System.out.println();

        jarEntryTaskWorkerCollection = WorkerCollection.create(4, "JarEntry-checker-", jarEntryTasks)
                .start();

        return this;
    }

    private static List<File> expandFile(File file) {
        if (file.isDirectory()) {
            return Arrays.stream(Objects.requireNonNull(file.listFiles()))
                    .map(JavaUsageChecker::expandFile)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList(file);
        }
    }

    public JavaUsageChecker join() {
        if (jarEntryTaskWorkerCollection == null)
            throw new IllegalStateException("Join called, but not started");
        jarEntryTaskWorkerCollection.join();
        return this;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void report(Report usage) {
        reports.add(usage);
    }

    public void reportMethodAccess(ClassLocation.Method.Code code, int opcode, String owner, String name, String descriptor, boolean isInterface) {
        owner = owner.replace('/', '.');

        for (Query query : queries) {
            if (query.checkMethodAccess(opcode, owner, name, descriptor, isInterface)) {
                // TODO include details of invoked method, same for others below
                report(new Report(code, query));
            }
        }
    }

    public void reportFieldAccess(ClassLocation.Method.Code code, int opcode, String owner, String name, String descriptor) {
        owner = owner.replace('/', '.');

        for (Query query : queries) {
            if (query.checkFieldAccess(opcode, owner, name, descriptor)) {
                report(new Report(code, query));
            }
        }
    }

    public void reportClassUsage(ClassLocation location, String name) {
        name = name.replace('/', '.');

        for (Query query : queries) {
            if (query.checkClassUsage(name)) {
                report(new Report(location, query));
            }
        }
    }

}
