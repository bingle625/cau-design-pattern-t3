package com.holub.life;

import java.util.Objects;

/**
 * Watches generations go by and asks the current {@link StopCondition}
 * whether the run should stop.
 *
 * <p>It observes two existing subjects and modifies neither. A clock tick
 * marks the start of a generation. The statistics notification that the
 * {@link Universe} publishes at the end of that tick marks its completion
 * and carries the live-cell count. Statistics published for other reasons
 * (a cell click, Clear, Load) are not preceded by a tick on the same thread
 * and are therefore not counted as generations.
 *
 * <p>The controller does not know the clock. It runs the stop action it was
 * given, so it can be tested without a clock or any UI.
 */
public final class AutoStopController implements Clock.Listener, StatisticsListener {
    private final Runnable stopAction;
    private StopCondition condition;
    private int generationsRun;
    private Thread tickingThread;

    /** @param stopAction what to do when the condition is met, for example stopping the clock */
    public AutoStopController(Runnable stopAction) {
        this.stopAction = Objects.requireNonNull(stopAction, "stopAction");
    }

    /** Sets the condition to watch and starts counting generations from zero. */
    public synchronized void arm(StopCondition newCondition) {
        condition = Objects.requireNonNull(newCondition, "newCondition");
        generationsRun = 0;
        tickingThread = null;
    }

    /** Removes the condition. The game then runs until the user halts it. */
    public synchronized void disarm() {
        condition = null;
        tickingThread = null;
    }

    /** @return the condition being watched, or null if there is none */
    public synchronized StopCondition armedCondition() {
        return condition;
    }

    /** A generation is starting on the calling thread. */
    @Override
    public synchronized void tick() {
        if (condition != null) {
            tickingThread = Thread.currentThread();
        }
    }

    /** A generation has been applied if a tick started on this same thread. */
    @Override
    public void statisticsChanged(GameStatistics statistics) {
        synchronized (this) {
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
        // Run outside the lock so that the action may call back into this controller.
        stopAction.run();
    }
}
