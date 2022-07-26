package com.github.tpgamesnl.javausagechecker.worker;

import java.util.Queue;

public class Worker<T extends Task> extends Thread {

    private final Queue<T> taskQueue;

    public Worker(Queue<T> taskQueue, String name) {
        super(name);
        this.taskQueue = taskQueue;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            T task;
            synchronized (taskQueue) {
                task = taskQueue.poll();
            }
            if (task == null) {
                return;
            }

            try {
                task.perform();
            } catch (Throwable e) {
                new RuntimeException("Task " + task + " threw an exception", e).printStackTrace();
            }
        }
    }
}
