package com.holub.life;

import java.util.Objects;

public final class AdvanceGenerationCommand implements Command {
    private final Cell board;
    private final Runnable refreshRequest;
    private BoardSnapshot before;
    private BoardSnapshot after;

    public AdvanceGenerationCommand(Cell board, Runnable refreshRequest) {
        this.board = Objects.requireNonNull(board, "board");
        this.refreshRequest = Objects.requireNonNull(refreshRequest, "refreshRequest");
    }

    @Override
    public void execute() {
        if (before != null) {
            throw new IllegalStateException("A generation command can only be executed once");
        }
        BoardSnapshot initialState = BoardSnapshot.capture(board);
        Cell outside = Cell.DUMMY;
        boolean refreshRequired = false;
        if (board.figureNextState(outside, outside, outside, outside,
                outside, outside, outside, outside)) {
            refreshRequired = board.transition();
        }
        BoardSnapshot completedState = BoardSnapshot.capture(board);
        if (refreshRequired) {
            refreshRequest.run();
        }
        before = initialState;
        after = completedState;
    }
}
