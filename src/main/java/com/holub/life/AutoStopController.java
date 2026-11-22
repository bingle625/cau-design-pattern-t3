package com.holub.life;

import java.util.Objects;

/** Counts generations and stops the game when the current {@link StopCondition} is met. */
public final class AutoStopController implements Clock.Listener, StatisticsListener {
    private final Runnable stopAction;
    private StopCondition condition;
    private int generationsRun;
    private Thread tickingThread;

    public AutoStopController(Runnable stopAction) {
        this.stopAction = Objects.requireNonNull(stopAction, "stopAction");
    }

    /** Starts counting generations from zero for the new condition. */
    public synchronized void arm(StopCondition newCondition) {
        condition = Objects.requireNonNull(newCondition, "newCondition");
        generationsRun = 0;
        tickingThread = null;
    }

    public synchronized void disarm() {
        condition = null;
        tickingThread = null;
    }

    /** @return the condition being watched, or null if there is none */
    public synchronized StopCondition armedCondition() {
        return condition;
    }

    @Override
    public synchronized void tick() {
        if (condition != null) {
            tickingThread = Thread.currentThread();
        }
    }

    @Override
    public void statisticsChanged(GameStatistics statistics) {
        synchronized (this) {
            // Statistics from a click, Clear or Load arrive without a tick on this thread.
            if (condition == null || tickingThread != Thread.currentThread()) {
                return;
            }
            tickingThread = null;
            ++generationsRun;
            if (!condition.shouldStop(generationsRun, statistics.getAliveCells())) {
                return;
            }
            condition = null;
        }
        // Outside the lock: the action may call back into this controller.
        stopAction.run();
    }
}
