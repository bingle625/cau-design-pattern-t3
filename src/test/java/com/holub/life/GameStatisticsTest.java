package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameStatisticsTest {

    @Test
    @DisplayName("A positive live cell count is stored and returned")
    void storesPositiveAliveCellCount() {
        GameStatistics statistics = new GameStatistics(10);

        assertEquals(10, statistics.getAliveCells());
    }

    @Test
    @DisplayName("Zero is a valid live cell count")
    void acceptsZeroAliveCellCount() {
        GameStatistics statistics = new GameStatistics(0);

        assertEquals(0, statistics.getAliveCells());
    }

    @Test
    @DisplayName("A negative live cell count is rejected")
    void rejectsNegativeAliveCellCount() {
        assertThrows(IllegalArgumentException.class, () -> new GameStatistics(-1));
    }

    @Test
    @DisplayName("Creating a new snapshot preserves the previous snapshot's count")
    void snapshotsRetainIndependentCounts() {
        GameStatistics previous = new GameStatistics(10);
        GameStatistics current = new GameStatistics(15);

        assertEquals(10, previous.getAliveCells());
        assertEquals(15, current.getAliveCells());
    }
}
