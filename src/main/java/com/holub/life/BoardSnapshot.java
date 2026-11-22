package com.holub.life;

import java.awt.Point;
import java.util.Objects;

/** Keeps a board's existing memento private for later restoration. */
public final class BoardSnapshot {
    private final Storable state;

    private BoardSnapshot(Storable state) {
        this.state = state;
    }

    public static BoardSnapshot capture(Cell board) {
        return new BoardSnapshot(Objects.requireNonNull(board, "board").createMemento());
    }

    /** Restores into a board with the same dimensions as the captured board. */
    public void restoreTo(Cell board) {
        Objects.requireNonNull(board, "board");
        board.clear();
        board.transfer(state, new Point(0, 0), Cell.LOAD);
    }
}
