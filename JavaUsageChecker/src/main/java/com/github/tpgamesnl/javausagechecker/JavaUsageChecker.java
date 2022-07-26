package com.github.tpgamesnl.javausagechecker;

import com.github.tpgamesnl.javausagechecker.query.Query;
import com.github.tpgamesnl.javausagechecker.worker.WorkerCollection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;

// TODO javadocs
public class JavaUsageChecker {

    @SuppressWarnings("UnusedReturnValue")
    public static class Builder {

        private final List<File> files = new ArrayList<>();
        private final List<Query> queries = new ArrayList<>();
        private int threadCount = 1;
        private StateTracker stateTracker;

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

        public Builder stateTracker(StateTracker stateTracker) {
            this.stateTracker = stateTracker;
            return this;
        }

        public JavaUsageChecker create() {
            return new JavaUsageChecker(files, queries, threadCount, stateTracker);
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

        public StateTracker getStateTracker() {
            return stateTracker;
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "files=" + files +
                    ", queries=" + queries +
                    ", threadCount=" + threadCount +
                    ", stateTracker=" + stateTracker +
                    '}';
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final List<File> files;
    private final List<Query> queries;
    private final List<Report> reports;

    private final int workerCount;

    private final StateTracker stateTracker;

    private WorkerCollection<JarEntryTask> jarEntryTaskWorkerCollection;

    public JavaUsageChecker(List<File> files, List<Query> queries, int workerCount, StateTracker stateTracker) {
        this.files = files;
        this.queries = queries;
        this.reports = new ArrayList<>();
        this.workerCount = workerCount;
        this.stateTracker = stateTracker;
    }

    public JavaUsageChecker start() {
        // TODO remove
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
            jarFileTasks.add(new JarFileTask(this, jarEntryTasks, file));
        }

        stateTracker.setState(StateTracker.State.OPENING_JARS);
        stateTracker.setTotalJarCount(jarFileTasks.size());

        WorkerCollection.create(workerCount, "JarFile-opener-", jarFileTasks)
                .start()
                .join();

        stateTracker.setState(StateTracker.State.CHECKING_CLASSES);
        stateTracker.setTotalClassCount(jarEntryTasks.size());

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

    public StateTracker getStateTracker() {
        return stateTracker;
    }

    public List<Report> getReports() {
        return reports;
    }

    public void report(Report usage) {
        reports.add(usage);
    }

    public static String formatClassName(String className) {
        className = className.replace('/', '.');
        if (className.endsWith(".class"))
            className = className.substring(0, className.length() - 6);
        return className;
    }

    public void reportMethodAccess(ClassLocation.Method.Code code, int opcode, String owner, String name, String descriptor, boolean isInterface) {
        owner = formatClassName(owner);

        for (Query query : queries) {
            if (query.checkMethodAccess(opcode, owner, name, descriptor, isInterface)) {
                // TODO include details of invoked method, same for others below
                report(new Report(code, query));
            }
        }
    }

    public void reportFieldAccess(ClassLocation.Method.Code code, int opcode, String owner, String name, String descriptor) {
        owner = formatClassName(owner);

        for (Query query : queries) {
            if (query.checkFieldAccess(opcode, owner, name, descriptor)) {
                report(new Report(code, query));
            }
        }
    }

    public void reportClassUsage(ClassLocation location, String name) {
        name = formatClassName(name);

        for (Query query : queries) {
            if (query.checkClassUsage(name)) {
                report(new Report(location, query));
            }
        }
    }

}
