package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdvanceGenerationCommandTest {

    @Test
    @DisplayName("An unchanged calculation skips transition and refresh")
    void skipsTransitionAndRefreshWhenCalculationIsUnchanged() {
        RecordingBoard board = new RecordingBoard(false, true);

        board.command().execute();

        assertEquals(List.of("calculate"), board.events);
    }

    @Test
    @DisplayName("A false transition result skips refresh after calculation")
    void skipsRefreshWhenTransitionReturnsFalse() {
        RecordingBoard board = new RecordingBoard(true, false);

        board.command().execute();

        assertEquals(List.of("calculate", "transition"), board.events);
    }

    @Test
    @DisplayName("A changing board is calculated, transitioned, and refreshed once in order")
    void refreshesOnceAfterTransition() {
        RecordingBoard board = new RecordingBoard(true, true);

        board.command().execute();

        assertEquals(List.of("calculate", "transition", "refresh"), board.events);
    }

    @Test
    @DisplayName("The outer board receives eight permanently dead boundary neighbors")
    void suppliesEightDummyNeighbors() {
        RecordingBoard board = new RecordingBoard(false, false);

        board.command().execute();

        Cell outside = Cell.DUMMY;
        assertArrayEquals(new Cell[] { outside, outside, outside, outside,
                outside, outside, outside, outside }, board.neighbors);
    }

    @Test
    @DisplayName("Each new command advances a real Blinker by exactly one generation without a UI")
    void advancesOneGenerationPerCommand() {
        Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
        Rectangle surface = new Rectangle(0, 0, 512, 512);
        for (int x = 3; x <= 5; x++) {
            board.userClicked(new Point(x * 8 + 1, 4 * 8 + 1), surface);
        }
        AtomicInteger refreshes = new AtomicInteger();

        new AdvanceGenerationCommand(board, refreshes::incrementAndGet).execute();

        assertLiveCells(board, new Point(4, 3), new Point(4, 4), new Point(4, 5));
        assertEquals(1, refreshes.get());

        new AdvanceGenerationCommand(board, refreshes::incrementAndGet).execute();

        assertLiveCells(board, new Point(3, 4), new Point(4, 4), new Point(5, 4));
        assertEquals(2, refreshes.get());
    }

    @Test
    @DisplayName("A command rejects a missing board or refresh callback at construction")
    void requiresBoardAndRefreshRequest() {
        assertThrows(NullPointerException.class,
                () -> new AdvanceGenerationCommand(null, () -> { }));
        assertThrows(NullPointerException.class,
                () -> new AdvanceGenerationCommand(Cell.DUMMY, null));
    }

    private static void assertLiveCells(Cell board, Point... expected) {
        assertEquals(expected.length, board.countAlive());
        Cell.Memento snapshot = (Cell.Memento) board.createMemento();
        for (Point position : expected) {
            assertTrue(snapshot.isAlive(position), "Live cell at " + position);
        }
    }

    /** Records only the two board operations a generation command is allowed to request. */
    private static final class RecordingBoard {
        private final List<String> events = new ArrayList<>();
        private final Cell cell;
        private Cell[] neighbors;

        private RecordingBoard(boolean calculationResult, boolean transitionResult) {
            cell = (Cell) Proxy.newProxyInstance(Cell.class.getClassLoader(), new Class<?>[] { Cell.class },
                    (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "figureNextState":
                                events.add("calculate");
                                neighbors = Arrays.copyOf(arguments, arguments.length, Cell[].class);
                                return calculationResult;
                            case "transition":
                                events.add("transition");
                                return transitionResult;
                            default:
                                throw new AssertionError("Unexpected board operation: " + method.getName());
                        }
                    });
        }

        private Command command() {
            return new AdvanceGenerationCommand(cell, () -> events.add("refresh"));
        }
    }
}
