package com.holub.life;

/**
 * Decides whether an automatic run should stop.
 * It is asked once after each generation has been applied to the board.
 */
@FunctionalInterface
public interface StopCondition {
    /**
     * @param generationsRun generations applied since this condition was set (1 for the first)
     * @param aliveCells     live cells on the board after the latest generation
     * @return true if the run should stop now
     */
    boolean shouldStop(int generationsRun, int aliveCells);
}
