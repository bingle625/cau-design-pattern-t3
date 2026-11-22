package com.holub.life;

/** Stops once the given number of generations has run. */
public final class GenerationLimitCondition implements StopCondition {
    private final int limit;

    public GenerationLimitCondition(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit must be at least 1");
        }
        this.limit = limit;
    }

    @Override
    public boolean shouldStop(int generationsRun, int aliveCells) {
        return generationsRun >= limit;
    }
}
