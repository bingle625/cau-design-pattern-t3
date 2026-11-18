package com.holub.life;

public final class GameStatistics {
    private final int aliveCells;

    public GameStatistics(int aliveCells) {
        if (aliveCells < 0) {
            throw new IllegalArgumentException("aliveCells must be non-negative");
        }
        this.aliveCells = aliveCells;
    }

    public int getAliveCells() {
        return aliveCells;
    }
}
