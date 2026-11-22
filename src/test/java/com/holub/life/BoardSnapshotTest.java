package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class BoardSnapshotTest {
    private static final int CELL_PX = 8;

    @Test
    @DisplayName("A captured board survives later clicks and restores only the saved live cells")
    void preservesStateAfterBoardChanges() {
        Cell board = newBoard(new Point(2, 3), new Point(10, 11));

        BoardSnapshot snapshot = BoardSnapshot.capture(board);

        assertLiveCells(board, new Point(2, 3), new Point(10, 11));
        click(board, new Point(2, 3));
        click(board, new Point(20, 21));
        snapshot.restoreTo(board);

        assertLiveCells(board, new Point(2, 3), new Point(10, 11));
    }

    @Test
    @DisplayName("Restoring an empty snapshot removes cells added after capture")
    void restoresEmptyBoard() {
        Cell board = newBoard();
        BoardSnapshot snapshot = BoardSnapshot.capture(board);
        click(board, new Point(3, 4));

        snapshot.restoreTo(board);

        assertLiveCells(board);
    }

    @Test
    @DisplayName("Independent snapshots can be restored repeatedly without consuming their state")
    void restoresIndependentSnapshotsRepeatedly() {
        Cell board = newBoard(new Point(2, 3));
        BoardSnapshot first = BoardSnapshot.capture(board);
        click(board, new Point(10, 11));
        BoardSnapshot second = BoardSnapshot.capture(board);
        board.clear();

        first.restoreTo(board);
        assertLiveCells(board, new Point(2, 3));
        second.restoreTo(board);
        assertLiveCells(board, new Point(2, 3), new Point(10, 11));
        first.restoreTo(board);
        assertLiveCells(board, new Point(2, 3));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("boundaryPatterns")
    @DisplayName("A restored boundary pattern evolves like the original board for three generations")
    void preservesEvolutionAcrossBlockBoundaries(String name, Point[] pattern) {
        Cell reference = newBoard(pattern);
        Cell restored = newBoard(pattern);
        BoardSnapshot snapshot = BoardSnapshot.capture(restored);
        advance(restored);

        snapshot.restoreTo(restored);

        for (int generation = 1; generation <= 3; generation++) {
            advance(reference);
            advance(restored);
            assertSameState(reference, restored, generation);
        }
    }

    private static Stream<Arguments> boundaryPatterns() {
        return Stream.of(
                Arguments.of("Blinker births across a vertical block boundary",
                        new Point[] { new Point(7, 3), new Point(7, 4), new Point(7, 5) }),
                Arguments.of("Blinker births across a horizontal block boundary",
                        new Point[] { new Point(3, 7), new Point(4, 7), new Point(5, 7) }),
                Arguments.of("Glider crosses the corner between four blocks",
                        new Point[] { new Point(7, 6), new Point(8, 7), new Point(6, 8),
                                new Point(7, 8), new Point(8, 8) }));
    }

    private static Cell newBoard(Point... positions) {
        Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
        for (Point position : positions) {
            click(board, position);
        }
        return board;
    }

    private static void click(Cell board, Point position) {
        int surfaceWidth = board.widthInCells() * CELL_PX;
        board.userClicked(new Point(position.x * CELL_PX + 1, position.y * CELL_PX + 1),
                new Rectangle(0, 0, surfaceWidth, surfaceWidth));
    }

    private static void advance(Cell board) {
        new AdvanceGenerationCommand(board, () -> { }).execute();
    }

    private static void assertLiveCells(Cell board, Point... expected) {
        assertEquals(expected.length, board.countAlive());
        Cell.Memento state = (Cell.Memento) board.createMemento();
        for (Point position : expected) {
            assertTrue(state.isAlive(position), "Live cell at " + position);
        }
    }

    private static void assertSameState(Cell expected, Cell actual, int generation) {
        assertEquals(expected.countAlive(), actual.countAlive(), "Live count at generation " + generation);
        Cell.Memento expectedState = (Cell.Memento) expected.createMemento();
        Cell.Memento actualState = (Cell.Memento) actual.createMemento();
        for (int y = 0; y < expected.widthInCells(); y++) {
            for (int x = 0; x < expected.widthInCells(); x++) {
                Point position = new Point(x, y);
                assertEquals(expectedState.isAlive(position), actualState.isAlive(position),
                        "Cell at " + position + " in generation " + generation);
            }
        }
    }
}
