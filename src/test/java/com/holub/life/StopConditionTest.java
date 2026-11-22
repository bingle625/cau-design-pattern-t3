package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Each stop condition is checked on its own, without a board, a clock, or any UI. */
class StopConditionTest {

    private static final int ANY_ALIVE = 7;
    private static final int ANY_GENERATION = 5;

    @Test
    @DisplayName("Generation limit keeps running before the limit is reached")
    void generationLimitKeepsRunningBeforeLimit() {
        StopCondition condition = new GenerationLimitCondition(10);

        assertFalse(condition.shouldStop(1, ANY_ALIVE));
        assertFalse(condition.shouldStop(9, ANY_ALIVE));
    }

    @Test
    @DisplayName("Generation limit stops exactly at the limit and stays stopped past it")
    void generationLimitStopsAtLimit() {
        StopCondition condition = new GenerationLimitCondition(10);

        assertTrue(condition.shouldStop(10, ANY_ALIVE));
        assertTrue(condition.shouldStop(11, ANY_ALIVE));
    }

    @Test
    @DisplayName("Generation limit ignores how many cells are alive")
    void generationLimitIgnoresPopulation() {
        StopCondition condition = new GenerationLimitCondition(3);

        assertFalse(condition.shouldStop(2, 0));
        assertTrue(condition.shouldStop(3, 0));
    }

    @Test
    @DisplayName("A generation limit below one is rejected")
    void generationLimitRejectsNonPositiveLimit() {
        assertThrows(IllegalArgumentException.class, () -> new GenerationLimitCondition(0));
        assertThrows(IllegalArgumentException.class, () -> new GenerationLimitCondition(-5));
    }

    @Test
    @DisplayName("Extinction stops only when no cell is alive")
    void extinctionStopsOnlyWhenBoardIsEmpty() {
        StopCondition condition = new ExtinctionCondition();

        assertTrue(condition.shouldStop(ANY_GENERATION, 0));
        assertFalse(condition.shouldStop(ANY_GENERATION, 1));
        assertFalse(condition.shouldStop(ANY_GENERATION, 4096));
    }

    @Test
    @DisplayName("Extinction ignores how many generations have run")
    void extinctionIgnoresGenerationCount() {
        StopCondition condition = new ExtinctionCondition();

        assertTrue(condition.shouldStop(1, 0));
        assertFalse(condition.shouldStop(1_000_000, 3));
    }

    @Test
    @DisplayName("Population threshold keeps running below the threshold")
    void populationThresholdKeepsRunningBelowThreshold() {
        StopCondition condition = new PopulationThresholdCondition(20);

        assertFalse(condition.shouldStop(ANY_GENERATION, 0));
        assertFalse(condition.shouldStop(ANY_GENERATION, 19));
    }

    @Test
    @DisplayName("Population threshold stops at the threshold and above it")
    void populationThresholdStopsAtThreshold() {
        StopCondition condition = new PopulationThresholdCondition(20);

        assertTrue(condition.shouldStop(ANY_GENERATION, 20));
        assertTrue(condition.shouldStop(ANY_GENERATION, 35));
    }

    @Test
    @DisplayName("A population threshold below one is rejected")
    void populationThresholdRejectsNonPositiveThreshold() {
        assertThrows(IllegalArgumentException.class, () -> new PopulationThresholdCondition(0));
        assertThrows(IllegalArgumentException.class, () -> new PopulationThresholdCondition(-1));
    }
}
