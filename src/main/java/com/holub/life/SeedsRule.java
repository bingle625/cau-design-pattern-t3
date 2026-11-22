package com.holub.life;

public final class SeedsRule implements LifeRule
{
	@Override
	public boolean nextAlive(boolean currentlyAlive, int livingNeighbors)
	{
		return !currentlyAlive && livingNeighbors == 2;
	}
}
