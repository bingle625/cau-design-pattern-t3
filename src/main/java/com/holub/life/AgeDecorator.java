package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * 살아 있는 세포를 오래 살아남을수록 기본 색에서 {@link #OLDEST_COLOR} 쪽으로
 * 바꾼다. 명암이 아니라 색상을 바꾸는 이유는, 몇 픽셀짜리 세포에서는
 * 같은 색의 명암 차이를 구분하기 어렵기 때문이다.
 */
public final class AgeDecorator extends RendererDecorator {
    /** 이 나이 이상인 세포는 모두 최고령 색으로 그린다. */
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
