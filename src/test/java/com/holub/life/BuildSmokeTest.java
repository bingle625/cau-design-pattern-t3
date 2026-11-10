package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the Gradle + JUnit setup can drive the original model classes
 * without any UI (no Universe, Clock, or MenuSite).
 */
class BuildSmokeTest {

    private static final Cell D = Cell.DUMMY;
    private static final int CELL_PX = 8;

    private static Cell newBoard() {
        return new Neighborhood(8, new Neighborhood(8, new Resident())); // 64x64, same as Universe
    }

    private static void click(Cell board, int x, int y) {
        board.userClicked(new Point(x * CELL_PX + 1, y * CELL_PX + 1),
                new Rectangle(0, 0, 64 * CELL_PX, 64 * CELL_PX));
    }

    private static void step(Cell board) {
        board.figureNextState(D, D, D, D, D, D, D, D);
        board.transition();
    }

    private static boolean alive(Cell board, int x, int y) {
        Cell.Memento m = (Cell.Memento) board.createMemento();
        return m.isAlive(new Point(x, y));
    }

    @Test
    @DisplayName("Board built like Universe is 64 cells wide")
    void boardWidth() {
        assertEquals(64, newBoard().widthInCells());
    }

    @Test
    @DisplayName("Blinker oscillates with period 2 (model runs headless)")
    void blinkerOscillates() {
        Cell board = newBoard();
        click(board, 3, 4);
        click(board, 4, 4);
        click(board, 5, 4);

        step(board); // horizontal -> vertical
        assertTrue(alive(board, 4, 3));
        assertTrue(alive(board, 4, 4));
        assertTrue(alive(board, 4, 5));
        assertFalse(alive(board, 3, 4));
        assertFalse(alive(board, 5, 4));

        step(board); // vertical -> horizontal
        assertTrue(alive(board, 3, 4));
        assertTrue(alive(board, 4, 4));
        assertTrue(alive(board, 5, 4));
        assertFalse(alive(board, 4, 3));
        assertFalse(alive(board, 4, 5));
    }
}
