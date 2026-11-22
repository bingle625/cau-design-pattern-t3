package com.holub.life;

/** Stops once no live cell is left on the board. */
public final class ExtinctionCondition implements StopCondition {
    @Override
    public boolean shouldStop(int generationsRun, int aliveCells) {
        return aliveCells == 0;
    }
}
