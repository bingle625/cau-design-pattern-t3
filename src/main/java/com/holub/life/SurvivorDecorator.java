package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/** 여러 세대를 살아남은 세포에 테두리를 그린다. */
public final class SurvivorDecorator extends RendererDecorator {
    /** 이 나이부터 장수 세포로 본다. */
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
