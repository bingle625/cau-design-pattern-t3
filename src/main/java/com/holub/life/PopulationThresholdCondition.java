package com.holub.life;

/** Stops once the number of live cells reaches the given threshold or more. */
public final class PopulationThresholdCondition implements StopCondition {
    private final int threshold;

    public PopulationThresholdCondition(int threshold) {
        if (threshold < 1) {
            throw new IllegalArgumentException("threshold must be at least 1");
        }
        this.threshold = threshold;
    }

    @Override
    public boolean shouldStop(int generationsRun, int aliveCells) {
        return aliveCells >= threshold;
    }
}
