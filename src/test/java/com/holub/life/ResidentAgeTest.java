package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ResidentAgeTest {
    private static final Cell D = Cell.DUMMY;
    private static final Rectangle SURFACE = new Rectangle(0, 0, 8, 8);

    /** 이웃 수와 무관하게 세포를 살려 둔다. */
    private static final LifeRule ALWAYS_ALIVE = (alive, neighbors) -> true;
    private static final LifeRule ALWAYS_DEAD = (alive, neighbors) -> false;

    @Test
    @DisplayName("A dead cell has no age")
    void deadCellHasNoAge() {
        assertEquals(0, new Resident().age());
    }

    @Test
    @DisplayName("A cell born by the rule starts at age one")
    void bornCellStartsAtOne() {
        Resident cell = new Resident(ALWAYS_ALIVE);

        advance(cell);

        assertEquals(1, cell.age());
    }

    @Test
    @DisplayName("A surviving cell grows one generation older on every transition")
    void survivingCellGrowsOlder() {
        Resident cell = new Resident(ALWAYS_ALIVE);

        advance(cell);
        advance(cell);
        advance(cell);

        assertEquals(3, cell.age());
    }

    @Test
    @DisplayName("A cell that dies loses its age")
    void dyingCellLosesItsAge() {
        Resident cell = new Resident(ALWAYS_ALIVE);
        advance(cell);
        advance(cell);

        cell.setRule(ALWAYS_DEAD);
        advance(cell);

        assertEquals(0, cell.age());
    }

    @Test
    @DisplayName("A cell reborn after dying starts again at age one")
    void rebornCellStartsOver() {
        Resident cell = new Resident(ALWAYS_ALIVE);
        advance(cell);
        advance(cell);
        cell.setRule(ALWAYS_DEAD);
        advance(cell);

        cell.setRule(ALWAYS_ALIVE);
        advance(cell);

        assertEquals(1, cell.age());
    }

    @Test
    @DisplayName("A clicked cell appears at age one and a second click removes it")
    void clickingTogglesAge() {
        Resident cell = new Resident();

        cell.userClicked(new Point(0, 0), SURFACE);
        assertEquals(1, cell.age());

        cell.userClicked(new Point(0, 0), SURFACE);
        assertEquals(0, cell.age());
    }

    @Test
    @DisplayName("Clearing a cell resets its age")
    void clearResetsAge() {
        Resident cell = new Resident(ALWAYS_ALIVE);
        advance(cell);
        advance(cell);

        cell.clear();

        assertEquals(0, cell.age());
    }

    @Test
    @DisplayName("Restoring a board from a memento restarts every live cell at age one")
    void restoringFromMementoRestartsAge() {
        Neighborhood board = new Neighborhood(2, new Resident());
        board.changeRule(ALWAYS_ALIVE);     // 멈춰 있던 보드도 함께 깨운다
        board.figureNextState(D, D, D, D, D, D, D, D);
        board.transition();
        board.figureNextState(D, D, D, D, D, D, D, D);
        board.transition();
        Resident corner = (Resident) board.edge(0, 0);
        assertEquals(2, corner.age());

        BoardSnapshot snapshot = BoardSnapshot.capture(board);
        snapshot.restoreTo(board);

        assertEquals(1, corner.age());
    }

    private static void advance(Resident cell) {
        cell.figureNextState(D, D, D, D, D, D, D, D);
        cell.transition();
    }
}
