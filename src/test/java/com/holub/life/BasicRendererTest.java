package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BasicRendererTest {
    private static final Rectangle CELL = new Rectangle(0, 0, 8, 8);

    private final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    private final Graphics graphics = image.getGraphics();
    private final CellRenderer renderer = new BasicRenderer();

    @Test
    @DisplayName("A live cell is filled with the live color")
    void liveCellUsesLiveColor() {
        renderer.draw(graphics, CELL, true, 1);

        assertEquals(BasicRenderer.LIVE_COLOR, colorAt(4, 4));
    }

    @Test
    @DisplayName("A dead cell is filled with the dead color")
    void deadCellUsesDeadColor() {
        renderer.draw(graphics, CELL, false, 0);

        assertEquals(BasicRenderer.DEAD_COLOR, colorAt(4, 4));
    }

    @Test
    @DisplayName("The grid line is drawn on the top and left edges only")
    void borderIsDrawnOnTopAndLeft() {
        renderer.draw(graphics, CELL, true, 1);

        assertEquals(BasicRenderer.BORDER_COLOR, colorAt(0, 4));
        assertEquals(BasicRenderer.BORDER_COLOR, colorAt(4, 0));
        assertEquals(BasicRenderer.LIVE_COLOR, colorAt(7, 7));
    }

    @Test
    @DisplayName("The age of a cell does not change the basic look")
    void ageDoesNotChangeTheColor() {
        renderer.draw(graphics, CELL, true, 30);

        assertEquals(BasicRenderer.LIVE_COLOR, colorAt(4, 4));
    }

    @Test
    @DisplayName("The caller's graphics color is restored after drawing")
    void graphicsColorIsRestored() {
        graphics.setColor(Color.MAGENTA);

        renderer.draw(graphics, CELL, true, 1);

        assertEquals(Color.MAGENTA, graphics.getColor());
    }

    private Color colorAt(int x, int y) {
        return new Color(image.getRGB(x, y), true);
    }
}
