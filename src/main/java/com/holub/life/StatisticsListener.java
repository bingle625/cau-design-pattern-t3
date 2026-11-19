package com.holub.life;

@FunctionalInterface
public interface StatisticsListener {
    void statisticsChanged(GameStatistics statistics);
}
