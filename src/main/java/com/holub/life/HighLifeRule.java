package com.holub.life;

public final class HighLifeRule implements LifeRule
{
	@Override
	public boolean nextAlive(boolean currentlyAlive, int livingNeighbors)
	{
		if (currentlyAlive)
			return livingNeighbors == 2 || livingNeighbors == 3;
		return livingNeighbors == 3 || livingNeighbors == 6;
	}
}
