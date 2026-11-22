package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommandHistoryTest {
    @Test
    @DisplayName("A command is recorded only after its execution returns normally")
    void recordsAfterSuccessfulExecution() {
        CommandHistory history = new CommandHistory();

        history.execute(command(() -> assertEquals(0, history.size(), "No record while execution is in progress")));

        assertEquals(1, history.size());
    }

    @Test
    @DisplayName("Commands execute in request order and each completed request is retained")
    void recordsConsecutiveRequests() {
        CommandHistory history = new CommandHistory();
        List<String> actions = new ArrayList<>();

        history.execute(command(() -> actions.add("first")));
        history.execute(command(() -> actions.add("second")));

        assertEquals(List.of("first", "second"), actions);
        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("A failed command does not add a record or evict a completed command")
    void preservesHistoryOnFailure() {
        CommandHistory history = new CommandHistory(1);
        history.execute(command(() -> { }));

        assertThrows(IllegalStateException.class, () -> history.execute(command(() -> {
            throw new IllegalStateException("Execution failed");
        })));

        assertEquals(1, history.size());
    }

    @Test
    @DisplayName("Clear discards the previous timeline and allows new recording")
    void clearsAndStartsNewTimeline() {
        CommandHistory history = new CommandHistory();
        history.execute(command(() -> { }));
        history.execute(command(() -> { }));

        history.clear();

        assertEquals(0, history.size());
        history.execute(command(() -> { }));
        assertEquals(1, history.size());
    }

    @Test
    @DisplayName("The default history retains at most ten completed generations")
    void limitsDefaultHistoryToTen() {
        CommandHistory history = new CommandHistory();

        for (int index = 0; index < 11; index++) {
            history.execute(command(() -> { }));
        }

        assertEquals(10, history.size());
    }

    @Test
    @DisplayName("A smaller configured capacity bounds recording as commands continue")
    void respectsConfiguredCapacity() {
        CommandHistory history = new CommandHistory(2);

        for (int index = 0; index < 4; index++) {
            history.execute(command(() -> { }));
        }

        assertEquals(2, history.size());
    }

    @Test
    @DisplayName("Invalid capacity and a missing command are rejected without recording")
    void rejectsInvalidRequests() {
        assertThrows(IllegalArgumentException.class, () -> new CommandHistory(0));
        assertThrows(IllegalArgumentException.class, () -> new CommandHistory(-1));
        CommandHistory history = new CommandHistory();

        assertThrows(NullPointerException.class, () -> history.execute(null));

        assertEquals(0, history.size());
    }

    @Test
    @DisplayName("Completed commands are undone in reverse execution order")
    void undoesInReverseOrder() {
        CommandHistory history = new CommandHistory();
        List<String> events = new ArrayList<>();
        history.execute(command(() -> { }, () -> events.add("first")));
        history.execute(command(() -> { }, () -> events.add("second")));

        assertTrue(history.undo());
        assertTrue(history.undo());

        assertEquals(List.of("second", "first"), events);
        assertEquals(0, history.size());
        assertFalse(history.undo());
    }

    @Test
    @DisplayName("Undo with no completed command leaves the history empty")
    void ignoresEmptyUndo() {
        CommandHistory history = new CommandHistory();

        assertFalse(history.undo());

        assertEquals(0, history.size());
    }

    @Test
    @DisplayName("A failed Undo keeps the same completed command available for retry")
    void retainsCommandWhenUndoFails() {
        CommandHistory history = new CommandHistory();
        AtomicInteger attempts = new AtomicInteger();
        history.execute(command(() -> { }, () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("Restore failed");
            }
        }));

        assertThrows(IllegalStateException.class, history::undo);
        assertEquals(1, history.size());
        assertTrue(history.undo());

        assertEquals(2, attempts.get());
        assertEquals(0, history.size());
    }

    @Test
    @DisplayName("Only the ten most recent commands can be undone after eleven executions")
    void undoesOnlyRetainedCommands() {
        CommandHistory history = new CommandHistory();
        List<Integer> undone = new ArrayList<>();
        for (int generation = 1; generation <= 11; generation++) {
            int recorded = generation;
            history.execute(command(() -> { }, () -> undone.add(recorded)));
        }

        for (int count = 0; count < 10; count++) {
            assertTrue(history.undo());
        }

        assertFalse(history.undo());
        assertEquals(List.of(11, 10, 9, 8, 7, 6, 5, 4, 3, 2), undone);
    }

    @Test
    @DisplayName("A new execution after Undo starts a branch and Clear removes all completed work")
    void handlesNewExecutionAndClearAfterUndo() {
        CommandHistory history = new CommandHistory();
        List<String> undone = new ArrayList<>();
        history.execute(command(() -> { }, () -> undone.add("first")));
        history.execute(command(() -> { }, () -> undone.add("second")));
        assertTrue(history.undo());
        history.execute(command(() -> { }, () -> undone.add("new")));

        assertTrue(history.undo());
        assertTrue(history.undo());
        assertEquals(List.of("second", "new", "first"), undone);
        history.execute(command(() -> { }));
        history.clear();
        assertFalse(history.undo());
    }

    @Test
    @DisplayName("Several real generations restore their earlier coordinates and can evolve again after Undo")
    void restoresRealGenerationStates() {
        Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
        for (Point position : new Point[] { new Point(7, 6), new Point(8, 7), new Point(6, 8),
                new Point(7, 8), new Point(8, 8) }) {
            board.userClicked(new Point(position.x * 8 + 1, position.y * 8 + 1),
                    new Rectangle(0, 0, 512, 512));
        }
        CommandHistory history = new CommandHistory();
        List<BoardSnapshot> states = new ArrayList<>();
        for (int generation = 0; generation < 3; generation++) {
            states.add(BoardSnapshot.capture(board));
            history.execute(new AdvanceGenerationCommand(board, () -> { }));
        }
        Cell expected = new Neighborhood(8, new Neighborhood(8, new Resident()));

        for (int generation = 2; generation >= 0; generation--) {
            assertTrue(history.undo());
            states.get(generation).restoreTo(expected);
            assertSameBoard(expected, board);
        }
        history.execute(new AdvanceGenerationCommand(board, () -> { }));
        new AdvanceGenerationCommand(expected, () -> { }).execute();
        assertSameBoard(expected, board);
    }

    private static void assertSameBoard(Cell expected, Cell actual) {
        assertEquals(expected.countAlive(), actual.countAlive());
        Cell.Memento first = (Cell.Memento) expected.createMemento();
        Cell.Memento second = (Cell.Memento) actual.createMemento();
        for (int y = 0; y < expected.widthInCells(); y++) {
            for (int x = 0; x < expected.widthInCells(); x++) {
                Point position = new Point(x, y);
                assertEquals(first.isAlive(position), second.isAlive(position), "Cell at " + position);
            }
        }
    }

    private static Command command(Runnable execute) {
        return command(execute, () -> { });
    }

    private static Command command(Runnable execute, Runnable undo) {
        return new Command() {
            @Override
            public void execute() {
                execute.run();
            }

            @Override
            public void undo() {
                undo.run();
            }
        };
    }

}
