package com.holub.life;

import java.util.Objects;

public final class AdvanceGenerationCommand implements Command {
    private final Cell board;
    private final Runnable refreshRequest;

    public AdvanceGenerationCommand(Cell board, Runnable refreshRequest) {
        this.board = Objects.requireNonNull(board, "board");
        this.refreshRequest = Objects.requireNonNull(refreshRequest, "refreshRequest");
    }

    @Override
    public void execute() {
        Cell outside = Cell.DUMMY;
        if (board.figureNextState(outside, outside, outside, outside,
                outside, outside, outside, outside)) {
            if (board.transition()) {
                refreshRequest.run();
            }
        }
    }
}
