package com.holub.life;

/** Decides, after each generation, whether an automatic run should stop. */
@FunctionalInterface
public interface StopCondition {
    /** @param generationsRun generations applied since this condition was set */
    boolean shouldStop(int generationsRun, int aliveCells);
}
