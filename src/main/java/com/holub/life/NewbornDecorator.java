package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/** 이번 세대에 나타난 세포에 테두리를 그린다. */
public final class NewbornDecorator extends RendererDecorator {
    static final Color FRAME_COLOR = Color.WHITE;

    public NewbornDecorator(CellRenderer inner) {
        super(inner);
    }

    @Override
    protected void decorate(Graphics g, Rectangle here, boolean alive, int age) {
        if (!alive || age != 1) {
            return;
        }
        g.setColor(FRAME_COLOR);
        outlineInterior(g, here);
    }
}
