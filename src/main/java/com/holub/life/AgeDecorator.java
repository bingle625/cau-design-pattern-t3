package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Shifts a live cell from the plain live color toward {@link #OLDEST_COLOR}
 * the longer it has stayed alive. A change of hue is used rather than a
 * darker shade, because shades of one color are hard to tell apart on
 * cells that are only a few pixels wide.
 */
public final class AgeDecorator extends RendererDecorator {
    /** Cells of this age or older are all drawn in the oldest color. */
    static final int OLDEST_AGE = 8;
    static final Color OLDEST_COLOR = Color.BLUE;

    private static final Color[] SHADES = new Color[OLDEST_AGE + 1];
    static {
        for (int age = 1; age <= OLDEST_AGE; ++age) {
            int alpha = 255 * (age - 1) / (OLDEST_AGE - 1);
            SHADES[age] = new Color(OLDEST_COLOR.getRed(), OLDEST_COLOR.getGreen(),
                    OLDEST_COLOR.getBlue(), alpha);
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
