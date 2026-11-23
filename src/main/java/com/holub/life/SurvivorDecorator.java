package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/** Frames the cells that have stayed alive for many generations. */
public final class SurvivorDecorator extends RendererDecorator {
    /** A cell counts as a survivor from this age on. */
    static final int SURVIVOR_AGE = 5;
    static final Color FRAME_COLOR = Color.CYAN;

    public SurvivorDecorator(CellRenderer inner) {
        super(inner);
    }

    @Override
    protected void decorate(Graphics g, Rectangle here, boolean alive, int age) {
        if (!alive || age < SURVIVOR_AGE) {
            return;
        }
        g.setColor(FRAME_COLOR);
        outlineInterior(g, here);
    }
}
