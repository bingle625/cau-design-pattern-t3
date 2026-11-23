package com.holub.life;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NeighborhoodRendererTest {
    private static final Cell D = Cell.DUMMY;
    private static final int CELL_SIZE = 8;

    private final Graphics graphics =
            new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB).getGraphics();

    @Test
    @DisplayName("A board cannot be left without a renderer")
    void rejectsMissingRenderer() {
        Neighborhood board = new Neighborhood(2, new Resident());

        assertThrows(NullPointerException.class, () -> board.changeRenderer(null));
    }

    @Test
    @DisplayName("A new renderer reaches every resident of a nested board")
    void rendererReachesEveryNestedResident() {
        Neighborhood board = new Neighborhood(2, new Neighborhood(2, new Resident()));
        RecordingRenderer renderer = new RecordingRenderer();

        board.changeRenderer(renderer);
        redrawAll(board);

        assertEquals(16, renderer.calls.size());
    }

    @Test
    @DisplayName("Each resident is drawn in its own place with its own state")
    void residentsReportTheirOwnStateAndBounds() {
        Neighborhood board = new Neighborhood(2, new Resident());
        RecordingRenderer renderer = new RecordingRenderer();
        board.changeRenderer(renderer);
        click(board, 1, 0);

        redrawAll(board);

        assertEquals(List.of(
                new Call(new Rectangle(0, 0, CELL_SIZE, CELL_SIZE), false, 0),
                new Call(new Rectangle(CELL_SIZE, 0, CELL_SIZE, CELL_SIZE), true, 1),
                new Call(new Rectangle(0, CELL_SIZE, CELL_SIZE, CELL_SIZE), false, 0),
                new Call(new Rectangle(CELL_SIZE, CELL_SIZE, CELL_SIZE, CELL_SIZE), false, 0)),
                renderer.calls);
    }

    @Test
    @DisplayName("Changing the renderer keeps the current generation and its ages")
    void changingRendererKeepsBoardState() {
        Neighborhood board = new Neighborhood(2, new Resident());
        board.changeRule((alive, neighbors) -> true);
        advance(board);
        advance(board);
        RecordingRenderer renderer = new RecordingRenderer();

        board.changeRenderer(renderer);
        redrawAll(board);

        assertTrue(renderer.calls.stream().allMatch(call -> call.alive() && call.age() == 2));
    }

    @Test
    @DisplayName("Cells created from a prototype keep the prototype's renderer")
    void createdCellsInheritRenderer() {
        Neighborhood prototype = new Neighborhood(2, new Resident());
        RecordingRenderer renderer = new RecordingRenderer();
        prototype.changeRenderer(renderer);

        Cell copy = prototype.create();
        copy.redraw(graphics, new Rectangle(0, 0, 2 * CELL_SIZE, 2 * CELL_SIZE), true);

        assertEquals(4, renderer.calls.size());
    }

    private void redrawAll(Neighborhood board) {
        int size = board.widthInCells() * CELL_SIZE;
        board.redraw(graphics, new Rectangle(0, 0, size, size), true);
    }

    private static void click(Neighborhood board, int column, int row) {
        int size = board.widthInCells() * CELL_SIZE;
        board.userClicked(new Point(column * CELL_SIZE, row * CELL_SIZE),
                new Rectangle(0, 0, size, size));
    }

    private static void advance(Neighborhood board) {
        board.figureNextState(D, D, D, D, D, D, D, D);
        board.transition();
    }

    private record Call(Rectangle here, boolean alive, int age) {
    }

    private static final class RecordingRenderer implements CellRenderer {
        private final List<Call> calls = new ArrayList<>();

        @Override
        public void draw(Graphics g, Rectangle here, boolean alive, int age) {
            calls.add(new Call(new Rectangle(here), alive, age));
        }
    }
}
