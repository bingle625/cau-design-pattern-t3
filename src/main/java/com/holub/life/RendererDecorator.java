package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Objects;

/**
 * Adds one visual effect on top of another renderer.
 * A decorator never repaints the cell: the wrapped renderer draws first
 * and the effect is only painted over it, so effects can be stacked
 * in any combination.
 */
public abstract class RendererDecorator implements CellRenderer {
    private final CellRenderer inner;

    protected RendererDecorator(CellRenderer inner) {
        this.inner = Objects.requireNonNull(inner, "inner");
    }

    @Override
    public final void draw(Graphics g, Rectangle here, boolean alive, int age) {
        inner.draw(g, here, alive, age);

        Color original = g.getColor();
        try {
            decorate(g, here, alive, age);
        } finally {
            g.setColor(original);
        }
    }

    /** Paint this decorator's effect over the already drawn cell. */
    protected abstract void decorate(Graphics g, Rectangle here, boolean alive, int age);

    /** Fill the cell's interior, leaving the grid line alone. */
    protected static void fillInterior(Graphics g, Rectangle here) {
        g.fillRect(here.x + 1, here.y + 1, here.width - 1, here.height - 1);
    }

    /** Draw a one pixel frame along the inner edge of the cell. */
    protected static void outlineInterior(Graphics g, Rectangle here) {
        g.drawRect(here.x + 1, here.y + 1, here.width - 2, here.height - 2);
    }
}
