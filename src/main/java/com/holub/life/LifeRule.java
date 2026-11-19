package com.holub.life;

/** Determines whether a cell will be alive in the next generation. */
public interface LifeRule
{
	boolean nextAlive(boolean currentlyAlive, int livingNeighbors);
}
