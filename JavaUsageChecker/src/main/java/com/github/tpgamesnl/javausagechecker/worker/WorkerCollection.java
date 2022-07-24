package com.github.tpgamesnl.javausagechecker.worker;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class WorkerCollection<T extends Task> {

    public static <T extends Task> WorkerCollection<T> create(int workerAmount, String prefix, Queue<T> queue) {
        List<Worker<T>> workers = new ArrayList<>();
        for (int i = 0; i < workerAmount; i++) {
            String name = prefix + (i + 1);
            Worker<T> worker = new Worker<>(queue, name);
            workers.add(worker);
        }
        return new WorkerCollection<>(workers);
    }

    private final List<Worker<T>> workers;

    public WorkerCollection(List<Worker<T>> workers) {
        this.workers = workers;
    }

    public WorkerCollection<T> start() {
        for (Worker<T> worker : workers) {
            worker.start();
        }
        return this;
    }

    public WorkerCollection<T> join() {
        for (Worker<T> worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

}
