package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The controller is driven the way the game drives it (a tick, then a
 * statistics notification) but with a counting stop action instead of a
 * real clock, so no UI is needed.
 */
class AutoStopControllerTest {

    private static final int ANY_ALIVE = 5;

    private AtomicInteger stops;
    private AutoStopController controller;

    @BeforeEach
    void createController() {
        stops = new AtomicInteger();
        controller = new AutoStopController(stops::incrementAndGet);
    }

    private void runGeneration(int aliveAfterwards) {
        controller.tick();
        controller.statisticsChanged(new GameStatistics(aliveAfterwards));
    }

    @Test
    @DisplayName("A generation limit of N stops after exactly N generations")
    void stopsAfterExactlyNGenerations() {
        controller.arm(new GenerationLimitCondition(3));

        runGeneration(ANY_ALIVE);
        runGeneration(ANY_ALIVE);
        assertEquals(0, stops.get(), "Must keep running after two of three generations");

        runGeneration(ANY_ALIVE);
        assertEquals(1, stops.get(), "Must stop after the third generation");
    }

    @Test
    @DisplayName("Without a condition the game is never stopped")
    void neverStopsWithoutCondition() {
        for (int i = 0; i < 100; i++) {
            runGeneration(0);
        }

        assertEquals(0, stops.get());
    }

    @Test
    @DisplayName("The condition is cleared once it has stopped the game")
    void conditionIsClearedAfterStopping() {
        StopCondition condition = new GenerationLimitCondition(1);
        controller.arm(condition);
        assertSame(condition, controller.armedCondition());

        runGeneration(ANY_ALIVE);
        runGeneration(ANY_ALIVE);
        runGeneration(ANY_ALIVE);

        assertEquals(1, stops.get(), "Later generations must not stop the game again");
        assertNull(controller.armedCondition());
    }

    @Test
    @DisplayName("Clearing the condition lets the game run on")
    void disarmingLetsTheGameRunOn() {
        controller.arm(new GenerationLimitCondition(2));
        runGeneration(ANY_ALIVE);

        controller.disarm();
        runGeneration(ANY_ALIVE);
        runGeneration(ANY_ALIVE);

        assertEquals(0, stops.get());
        assertNull(controller.armedCondition());
    }

    @Test
    @DisplayName("Statistics published by a click, Clear or Load are not counted as generations")
    void statisticsWithoutTickAreNotGenerations() {
        controller.arm(new GenerationLimitCondition(2));

        controller.statisticsChanged(new GameStatistics(ANY_ALIVE));
        controller.statisticsChanged(new GameStatistics(ANY_ALIVE));
        controller.statisticsChanged(new GameStatistics(ANY_ALIVE));
        assertEquals(0, stops.get(), "Three edits are not three generations");

        runGeneration(ANY_ALIVE);
        runGeneration(ANY_ALIVE);
        assertEquals(1, stops.get(), "Two real generations reach the limit");
    }

    @Test
    @DisplayName("A board edit on another thread during a tick is not counted as that generation")
    void statisticsFromAnotherThreadAreNotGenerations() throws Exception {
        controller.arm(new GenerationLimitCondition(1));
        controller.tick();

        Thread editingThread = new Thread(
                () -> controller.statisticsChanged(new GameStatistics(ANY_ALIVE)));
        editingThread.start();
        editingThread.join();
        assertEquals(0, stops.get(), "The edit must not complete the generation");

        controller.statisticsChanged(new GameStatistics(ANY_ALIVE));
        assertEquals(1, stops.get(), "The ticking thread's own notification completes it");
    }

    @Test
    @DisplayName("Setting a new condition restarts the generation count")
    void armingAgainRestartsTheCount() {
        controller.arm(new GenerationLimitCondition(3));
        runGeneration(ANY_ALIVE);
        runGeneration(ANY_ALIVE);

        controller.arm(new GenerationLimitCondition(3));
        runGeneration(ANY_ALIVE);
        runGeneration(ANY_ALIVE);
        assertEquals(0, stops.get(), "Only two generations have run since the new condition");

        runGeneration(ANY_ALIVE);
        assertEquals(1, stops.get());
    }

    @Test
    @DisplayName("The condition is given the generation number and the live-cell count")
    void conditionReceivesGenerationAndPopulation() {
        List<String> seen = new ArrayList<>();
        controller.arm((generationsRun, aliveCells) -> {
            seen.add(generationsRun + ":" + aliveCells);
            return false;
        });

        runGeneration(12);
        runGeneration(9);
        runGeneration(0);

        assertEquals(List.of("1:12", "2:9", "3:0"), seen);
    }

    @Test
    @DisplayName("Extinction stops in the generation where the last cell dies")
    void extinctionStopsWhenLastCellDies() {
        controller.arm(new ExtinctionCondition());

        runGeneration(3);
        runGeneration(1);
        assertEquals(0, stops.get());

        runGeneration(0);
        assertEquals(1, stops.get());
    }

    @Test
    @DisplayName("The stop action may set a new condition")
    void stopActionMaySetNewCondition() {
        StopCondition next = new ExtinctionCondition();
        AutoStopController[] holder = new AutoStopController[1];
        holder[0] = new AutoStopController(() -> holder[0].arm(next));
        holder[0].arm(new GenerationLimitCondition(1));

        holder[0].tick();
        holder[0].statisticsChanged(new GameStatistics(ANY_ALIVE));

        assertSame(next, holder[0].armedCondition());
    }

    @Test
    @DisplayName("A missing condition or stop action is rejected")
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> controller.arm(null));
        assertThrows(NullPointerException.class, () -> new AutoStopController(null));
    }
}
