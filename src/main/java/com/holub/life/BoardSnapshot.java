package com.holub.life;

import java.awt.Point;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Keeps a board's existing memento private for later restoration.
 * The memento holds only which cells are alive, so the cells' ages are
 * kept alongside it; Undo and Redo then bring back the same ages the
 * board had when it was captured.
 */
public final class BoardSnapshot {
    private final Storable state;
    /** Ages in the board's own cell order, or null for boards without residents to visit. */
    private final int[] ages;

    private BoardSnapshot(Storable state, int[] ages) {
        this.state = state;
        this.ages = ages;
    }

    public static BoardSnapshot capture(Cell board) {
        Objects.requireNonNull(board, "board");
        return new BoardSnapshot(board.createMemento(), captureAges(board));
    }

    /** Restores into a board with the same dimensions as the captured board. */
    public void restoreTo(Cell board) {
        Objects.requireNonNull(board, "board");
        board.clear();
        board.transfer(state, new Point(0, 0), Cell.LOAD);
        restoreAges(board);
    }

    private static int[] captureAges(Cell board) {
        if (board instanceof Neighborhood neighborhood) {
            IntStream.Builder ages = IntStream.builder();
            neighborhood.collectAges(ages);
            return ages.build().toArray();
        }
        if (board instanceof Resident resident) {
            return new int[] { resident.age() };
        }
        return null;
    }

    private void restoreAges(Cell board) {
        if (ages == null) {
            return;
        }
        if (board instanceof Neighborhood neighborhood) {
            neighborhood.restoreAges(Arrays.stream(ages).iterator());
        } else if (board instanceof Resident resident) {
            resident.restoreAge(ages[0]);
        }
    }
}
