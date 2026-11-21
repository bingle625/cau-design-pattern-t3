package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class LifeRuleTest {
    private static final Cell D = Cell.DUMMY;

    @Test
    void rulesMatchTheirBirthAndSurvivalTables() {
        assertRule(new ConwayRule(), Set.of(3), Set.of(2, 3));
        assertRule(new HighLifeRule(), Set.of(3, 6), Set.of(2, 3));
        assertRule(new SeedsRule(), Set.of(2), Set.of());
    }

    @Test
    void switchingRuleWakesAStableBlockAcrossNeighborhoodBoundaries() {
        Neighborhood board = new Neighborhood(2, new Neighborhood(8, new Resident()));
        Set<Point> block = Set.of(new Point(7, 7), new Point(8, 7),
                new Point(7, 8), new Point(8, 8));
        seed(board, block);

        assertFalse(calculateNextState(board), "Conway keeps the block stable");
        assertFalse(calculateNextState(board), "Stable regions can now skip calculation");
        assertEquals(block, liveCells(board));

        board.changeRule(new SeedsRule());

        assertEquals(block, liveCells(board), "Changing rule preserves the current generation");
        assertTrue(calculateNextState(board));
        board.transition();
        assertEquals(Set.of(
                new Point(7, 6), new Point(8, 6), new Point(7, 9), new Point(8, 9),
                new Point(6, 7), new Point(6, 8), new Point(9, 7), new Point(9, 8)),
                liveCells(board));
    }

    @Test
    void loadingWithSeedsEvaluatesAnEmptyAdjacentNeighborhood() {
        Neighborhood board = new Neighborhood(2, new Neighborhood(8, new Resident(new SeedsRule())));
        Set<Point> savedCells = Set.of(new Point(7, 3), new Point(7, 4));
        seed(board, savedCells);
        Storable saved = board.createMemento();
        board.clear();
        board.transfer(saved, new Point(0, 0), Cell.LOAD);
        assertEquals(savedCells, liveCells(board));

        assertTrue(calculateNextState(board));
        board.transition();
        assertEquals(Set.of(new Point(6, 3), new Point(6, 4),
                new Point(8, 3), new Point(8, 4)), liveCells(board));
    }

    @Test
    void clonedResidentsUseSelectedRuleAndReportAnEntireBoardDying() {
        Neighborhood board = new Neighborhood(2, new Resident(new SeedsRule()));
        seed(board, Set.of(new Point(0, 0), new Point(1, 0),
                new Point(0, 1), new Point(1, 1)));

        assertTrue(calculateNextState(board));
        assertTrue(board.transition(), "The caller must redraw after every cell dies");
        assertEquals(Set.of(), liveCells(board));
    }

    private static void assertRule(LifeRule rule, Set<Integer> birth, Set<Integer> survival) {
        for (int neighbors = 0; neighbors <= 8; neighbors++) {
            assertEquals(birth.contains(neighbors), rule.nextAlive(false, neighbors),
                    rule.getClass().getSimpleName() + " birth with " + neighbors + " neighbors");
            assertEquals(survival.contains(neighbors), rule.nextAlive(true, neighbors),
                    rule.getClass().getSimpleName() + " survival with " + neighbors + " neighbors");
        }
    }

    private static void seed(Neighborhood board, Set<Point> cells) {
        int width = board.widthInCells();
        Rectangle surface = new Rectangle(0, 0, width, width);
        for (Point cell : cells) {
            board.userClicked(cell, surface);
        }
    }

    private static boolean calculateNextState(Neighborhood board) {
        return board.figureNextState(D, D, D, D, D, D, D, D);
    }

    private static Set<Point> liveCells(Neighborhood board) {
        Cell.Memento snapshot = (Cell.Memento) board.createMemento();
        Set<Point> cells = new HashSet<>();
        for (int y = 0; y < board.widthInCells(); y++) {
            for (int x = 0; x < board.widthInCells(); x++) {
                Point cell = new Point(x, y);
                if (snapshot.isAlive(cell)) {
                    cells.add(cell);
                }
            }
        }
        return cells;
    }
}
