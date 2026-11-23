package com.holub.life;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * 보드의 세포 하나를 그린다.
 * 구현체는 Graphics의 색을 호출 전 상태로 되돌려 놓아야 한다.
 */
public interface CellRenderer {
    /**
     * @param g     그릴 대상 graphics
     * @param here  세포의 영역
     * @param alive 세포가 현재 살아 있는지 여부
     * @param age   세포가 살아남은 세대 수. 죽었으면 0,
     *              방금 나타났으면 1
     */
    void draw(Graphics g, Rectangle here, boolean alive, int age);
}
