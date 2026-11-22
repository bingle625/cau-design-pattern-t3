package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Field;
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

        assertEquals(List.of("capture", "calculate", "capture"), board.events);
    }

    @Test
    @DisplayName("A false transition result skips refresh after calculation")
    void skipsRefreshWhenTransitionReturnsFalse() {
        RecordingBoard board = new RecordingBoard(true, false);

        board.command().execute();

        assertEquals(List.of("capture", "calculate", "transition", "capture"), board.events);
    }

    @Test
    @DisplayName("A changing board is calculated, transitioned, and refreshed once in order")
    void refreshesOnceAfterTransition() {
        RecordingBoard board = new RecordingBoard(true, true);

        board.command().execute();

        assertEquals(List.of("capture", "calculate", "transition", "capture", "refresh"), board.events);
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

    @Test
    @DisplayName("Each command keeps its execution-time before and after states when later commands run")
    void retainsIndependentExecutionStates() throws Exception {
        Cell board = newBoard(new Point(3, 4), new Point(4, 4));
        AdvanceGenerationCommand first = new AdvanceGenerationCommand(board, () -> { });
        click(board, new Point(5, 4));

        first.execute();
        AdvanceGenerationCommand second = new AdvanceGenerationCommand(board, () -> { });
        second.execute();
        board.clear();

        Cell restored = newBoard();
        snapshot(first, "before").restoreTo(restored);
        assertLiveCells(restored, new Point(3, 4), new Point(4, 4), new Point(5, 4));
        snapshot(first, "after").restoreTo(restored);
        assertLiveCells(restored, new Point(4, 3), new Point(4, 4), new Point(4, 5));
        snapshot(second, "before").restoreTo(restored);
        assertLiveCells(restored, new Point(4, 3), new Point(4, 4), new Point(4, 5));
        snapshot(second, "after").restoreTo(restored);
        assertLiveCells(restored, new Point(3, 4), new Point(4, 4), new Point(5, 4));
    }

    @Test
    @DisplayName("Reusing a completed command is rejected without advancing or overwriting its record")
    void rejectsSecondExecution() {
        RecordingBoard board = new RecordingBoard(true, true);
        Command command = board.command();
        command.execute();
        List<String> firstExecution = List.copyOf(board.events);

        assertThrows(IllegalStateException.class, command::execute);

        assertEquals(firstExecution, board.events);
    }

    @Test
    @DisplayName("Undo restores the board before execution without requesting a UI refresh")
    void undoRestoresPreviousBoard() {
        Cell board = newBoard(new Point(3, 4));
        AtomicInteger refreshes = new AtomicInteger();
        Command command = new AdvanceGenerationCommand(board, refreshes::incrementAndGet);
        command.execute();
        assertLiveCells(board);
        int requestsAfterExecution = refreshes.get();

        command.undo();

        assertLiveCells(board, new Point(3, 4));
        assertEquals(requestsAfterExecution, refreshes.get(), "The UI entry point refreshes after Undo");
    }

    @Test
    @DisplayName("Undo before a completed execution is rejected without modifying the board")
    void rejectsUndoBeforeExecution() {
        Cell board = newBoard(new Point(3, 4));
        Command command = new AdvanceGenerationCommand(board, () -> { });

        assertThrows(IllegalStateException.class, command::undo);

        assertLiveCells(board, new Point(3, 4));
    }

    @Test
    @DisplayName("Capturing generation states preserves the legacy board evolution across block boundaries")
    void matchesLegacyEvolution() {
        Point[][] patterns = {
                { new Point(7, 3), new Point(7, 4), new Point(7, 5) },
                { new Point(7, 6), new Point(8, 7), new Point(6, 8), new Point(7, 8), new Point(8, 8) }
        };
        for (Point[] pattern : patterns) {
            Cell reference = newBoard(pattern);
            Cell actual = newBoard(pattern);
            for (int generation = 1; generation <= 8; generation++) {
                Cell outside = Cell.DUMMY;
                if (reference.figureNextState(outside, outside, outside, outside,
                        outside, outside, outside, outside)) {
                    reference.transition();
                }
                new AdvanceGenerationCommand(actual, () -> { }).execute();

                assertEquals(reference.countAlive(), actual.countAlive());
                Cell.Memento expected = (Cell.Memento) reference.createMemento();
                Cell.Memento received = (Cell.Memento) actual.createMemento();
                for (int y = 0; y < reference.widthInCells(); y++) {
                    for (int x = 0; x < reference.widthInCells(); x++) {
                        Point position = new Point(x, y);
                        assertEquals(expected.isAlive(position), received.isAlive(position),
                                "Cell " + position + " at generation " + generation);
                    }
                }
            }
        }
    }

    private static Cell newBoard(Point... positions) {
        Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
        for (Point position : positions) {
            click(board, position);
        }
        return board;
    }

    private static void click(Cell board, Point position) {
        int width = board.widthInCells() * 8;
        board.userClicked(new Point(position.x * 8 + 1, position.y * 8 + 1),
                new Rectangle(0, 0, width, width));
    }

    // Inspect preparation state until the public Undo/Redo operations are added in the next units.
    private static BoardSnapshot snapshot(AdvanceGenerationCommand command, String name) throws Exception {
        Field field = AdvanceGenerationCommand.class.getDeclaredField(name);
        field.setAccessible(true);
        return (BoardSnapshot) field.get(command);
    }

    private static void assertLiveCells(Cell board, Point... expected) {
        assertEquals(expected.length, board.countAlive());
        Cell.Memento snapshot = (Cell.Memento) board.createMemento();
        for (Point position : expected) {
            assertTrue(snapshot.isAlive(position), "Live cell at " + position);
        }
    }

    /** Records state capture, calculation, transition, and refresh requests. */
    private static final class RecordingBoard {
        private final List<String> events = new ArrayList<>();
        private final Cell cell;
        private Cell[] neighbors;

        private RecordingBoard(boolean calculationResult, boolean transitionResult) {
            cell = (Cell) Proxy.newProxyInstance(Cell.class.getClassLoader(), new Class<?>[] { Cell.class },
                    (proxy, method, arguments) -> {
                        switch (method.getName()) {
                            case "createMemento":
                                events.add("capture");
                                return new Neighborhood(1, new Resident()).createMemento();
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
