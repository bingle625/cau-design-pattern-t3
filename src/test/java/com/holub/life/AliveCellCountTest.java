package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Point;
import java.awt.Rectangle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AliveCellCountTest {

    private static final int CELL_PX = 8;

    @Test
    @DisplayName("The board boundary contributes zero live cells")
    void dummyCountsAsZero() {
        assertEquals(0, Cell.DUMMY.countAlive());
    }

    @Test
    @DisplayName("Clicking a resident twice toggles its count from zero to one and back")
    void residentCountFollowsClicks() {
        Cell resident = new Resident();
        assertEquals(0, resident.countAlive());

        click(resident, 0, 0);
        assertEquals(1, resident.countAlive());

        click(resident, 0, 0);
        assertEquals(0, resident.countAlive());
    }

    @Test
    @DisplayName("A nested board counts live cells across separate neighborhoods")
    void nestedNeighborhoodSumsLiveCellsAcrossBlocks() {
        Cell board = newBoard();
        assertEquals(0, board.countAlive());

        click(board, 2, 3);
        click(board, 10, 3);
        click(board, 2, 11);
        assertEquals(3, board.countAlive());

        click(board, 10, 3);
        assertEquals(2, board.countAlive());
    }

    @Test
    @DisplayName("Computing the next generation leaves the current live count intact")
    void figureNextStateKeepsCurrentCount() {
        Cell board = newBoard();
        click(board, 3, 4);
        assertEquals(1, board.countAlive());

        assertTrue(figureNextState(board));
        assertEquals(1, board.countAlive());
    }

    @Test
    @DisplayName("Applying the next generation removes an isolated live cell from the count")
    void transitionUpdatesCountToNextGeneration() {
        Cell board = newBoard();
        click(board, 3, 4);
        assertEquals(1, board.countAlive());

        assertTrue(figureNextState(board));
        board.transition();
        assertEquals(0, board.countAlive());
    }

    @Test
    @DisplayName("Clearing a populated board resets its live count to zero")
    void clearResetsLiveCount() {
        Cell board = newBoard();
        click(board, 2, 3);
        click(board, 10, 11);
        assertEquals(2, board.countAlive());

        board.clear();
        assertEquals(0, board.countAlive());
    }

    @Test
    @DisplayName("Restoring a memento recovers the live count and cell positions")
    void mementoRestoreRecoversCountAndPositions() {
        Cell board = newBoard();
        Point first = new Point(2, 3);
        Point second = new Point(10, 11);
        click(board, first.x, first.y);
        click(board, second.x, second.y);
        int savedCount = board.countAlive();
        assertEquals(2, savedCount);
        Storable memento = board.createMemento();

        board.clear();
        assertEquals(0, board.countAlive());

        board.transfer(memento, new Point(0, 0), Cell.LOAD);

        assertEquals(savedCount, board.countAlive());
        Cell.Memento restored = (Cell.Memento) board.createMemento();
        assertTrue(restored.isAlive(first));
        assertTrue(restored.isAlive(second));
        assertFalse(restored.isAlive(new Point(first.x, second.y)));
    }

    private static Cell newBoard() {
        return new Neighborhood(8, new Neighborhood(8, new Resident()));
    }

    private static void click(Cell cell, int x, int y) {
        int surfaceWidth = cell.widthInCells() * CELL_PX;
        cell.userClicked(new Point(x * CELL_PX + 1, y * CELL_PX + 1),
                new Rectangle(0, 0, surfaceWidth, surfaceWidth));
    }

    private static boolean figureNextState(Cell board) {
        Cell outside = Cell.DUMMY;
        return board.figureNextState(outside, outside, outside, outside,
                outside, outside, outside, outside);
    }
}
