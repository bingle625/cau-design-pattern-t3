package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Objects;

/**
 * 다른 렌더러 위에 시각 효과 하나를 덧입힌다.
 * 데코레이터는 세포를 다시 칠하지 않는다. 감싼 렌더러가 먼저 그리고
 * 효과는 그 위에 덧그리기만 하므로, 어떤 조합으로든 겹쳐 쓸 수 있다.
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

    /** 이미 그려진 세포 위에 이 데코레이터의 효과를 덧그린다. */
    protected abstract void decorate(Graphics g, Rectangle here, boolean alive, int age);

    /** 격자선은 그대로 두고 세포 안쪽을 채운다. */
    protected static void fillInterior(Graphics g, Rectangle here) {
        g.fillRect(here.x + 1, here.y + 1, here.width - 1, here.height - 1);
    }

    /** 세포 안쪽 가장자리를 따라 1픽셀 테두리를 그린다. */
    protected static void outlineInterior(Graphics g, Rectangle here) {
        g.drawRect(here.x + 1, here.y + 1, here.width - 2, here.height - 2);
    }
}
