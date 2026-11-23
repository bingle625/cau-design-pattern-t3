package com.holub.life;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Draws one cell of the board.
 * Implementations must leave the color of the Graphics as they found it.
 */
public interface CellRenderer {
    /**
     * @param g     draw onto this graphics
     * @param here  bounds of the cell
     * @param alive whether the cell is currently alive
     * @param age   generations the cell has stayed alive; 0 when dead,
     *              1 when it has just appeared
     */
    void draw(Graphics g, Rectangle here, boolean alive, int age);
}
