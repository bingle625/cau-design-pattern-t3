package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import com.holub.ui.Colors;

/** 원본의 세포 표현. 격자선 안쪽을 생사에 따른 단색으로 채운다. */
public final class BasicRenderer implements CellRenderer {
    static final Color BORDER_COLOR = Colors.DARK_YELLOW;
    static final Color LIVE_COLOR = Color.RED;
    static final Color DEAD_COLOR = Colors.LIGHT_YELLOW;

    @Override
    public void draw(Graphics g, Rectangle here, boolean alive, int age) {
        Color original = g.getColor();
        g.setColor(alive ? LIVE_COLOR : DEAD_COLOR);
        g.fillRect(here.x + 1, here.y + 1, here.width - 1, here.height - 1);

        // Doesn't draw a line on the far right and bottom of the
        // grid, but that's life, so to speak. It's not worth the
        // code for the special case.

        g.setColor(BORDER_COLOR);
        g.drawLine(here.x, here.y, here.x, here.y + here.height);
        g.drawLine(here.x, here.y, here.x + here.width, here.y);
        g.setColor(original);
    }
}
