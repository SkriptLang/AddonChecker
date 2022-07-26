package com.github.tpgamesnl.javausagechecker;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class StateTracker {

    public enum State {
        STOPPED,
        OPENING_JARS,
        CHECKING_CLASSES
    }

    private final AtomicInteger jarOpenedCount = new AtomicInteger();
    private final AtomicInteger classesCheckedCount = new AtomicInteger();
    private final AtomicReference<State> state = new AtomicReference<>(State.STOPPED);
    private int totalJarCount = -1;
    private int totalClassCount = -1;

    public void setState(State state) {
        this.state.set(state);
        updateState(state);
    }

    protected State getState() {
        return this.state.get();
    }

    abstract void updateState(State state);

    abstract void jarOpenedCountUpdated(int count);

    abstract void classesCheckedCountUpdated(int count);

    public void incrementJarOpenedCount() {
        int newCount = jarOpenedCount.incrementAndGet();
        jarOpenedCountUpdated(newCount);
    }

    public void incrementClassesCheckedCount() {
        int newCount = classesCheckedCount.incrementAndGet();
        classesCheckedCountUpdated(newCount);
    }

    protected int getTotalJarCount() {
        return totalJarCount;
    }

    protected int getTotalClassCount() {
        return totalClassCount;
    }

    public void setTotalJarCount(int totalJarCount) {
        this.totalJarCount = totalJarCount;
    }

    public void setTotalClassCount(int totalClassCount) {
        this.totalClassCount = totalClassCount;
    }

    @Override
    public String toString() {
        return "StateTracker{" +
                "jarOpenedCount=" + jarOpenedCount +
                ", classesCheckedCount=" + classesCheckedCount +
                ", totalJarCount=" + totalJarCount +
                ", totalClassCount=" + totalClassCount +
                '}';
    }

}
