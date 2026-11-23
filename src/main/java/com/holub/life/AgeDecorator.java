package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/** Darkens a live cell the longer it has stayed alive. */
public final class AgeDecorator extends RendererDecorator {
    /** Cells of this age or older all get the darkest shade. */
    static final int OLDEST_AGE = 10;
    private static final int DARKEST_ALPHA = 180;

    private static final Color[] SHADES = new Color[OLDEST_AGE + 1];
    static {
        for (int age = 1; age <= OLDEST_AGE; ++age) {
            int alpha = DARKEST_ALPHA * (age - 1) / (OLDEST_AGE - 1);
            SHADES[age] = new Color(0, 0, 0, alpha);
        }
    }

    public AgeDecorator(CellRenderer inner) {
        super(inner);
    }

    @Override
    protected void decorate(Graphics g, Rectangle here, boolean alive, int age) {
        if (!alive || age <= 1) {
            return;
        }
        g.setColor(SHADES[Math.min(age, OLDEST_AGE)]);
        fillInterior(g, here);
    }
}
