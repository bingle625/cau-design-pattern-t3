package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.holub.ui.MenuSite;

@Execution(ExecutionMode.SAME_THREAD)
class UniverseClickStatisticsGuiTest {

    private static final int CELL_PX = 8;
    private static JFrame frame;
    private static Universe universe;
    private static Clock clock;

    private final List<Integer> receivedCounts = new ArrayList<>();
    private final StatisticsListener listener = statistics -> receivedCounts.add(statistics.getAliveCells());
    private boolean subscribed;

    @BeforeAll
    static void initializeUniverse() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame();
            MenuSite.establish(frame);
            universe = Universe.instance();
            clock = Clock.instance();
            frame.add(universe);
            frame.pack();
        });
    }

    @BeforeEach
    void resetBoardAndSubscribe() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            clearBoard();
            universe.addStatisticsListener(listener);
            subscribed = true;
        });
    }

    @AfterEach
    void cleanUpGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            try {
                if (subscribed) {
                    unsubscribe();
                }
            } finally {
                clearBoard();
            }
        });
    }

    @AfterAll
    static void disposeFrame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (frame != null) {
                frame.dispose();
            }
        });
    }

    @Test
    @DisplayName("Clicking a dead cell publishes one snapshot with one live cell")
    void publishesOneAfterClickingDeadCell() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(2, 3);

            assertEquals(List.of(1), receivedCounts, "Snapshots after one click on an empty board");
        });
    }

    @Test
    @DisplayName("Clicking the same cell twice publishes one and then zero")
    void publishesZeroAfterClickingSameCellAgain() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(2, 3);
            assertEquals(List.of(1), receivedCounts, "Snapshots after the first click");

            clickCell(2, 3);
            assertEquals(List.of(1, 0), receivedCounts, "Snapshots after the second click");
        });
    }

    @Test
    @DisplayName("Clicking three distinct cells publishes the cumulative counts in order")
    void publishesCumulativeCountsAcrossThreeCells() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(2, 3);
            assertEquals(List.of(1), receivedCounts, "Snapshots after the first cell");

            clickCell(10, 11);
            assertEquals(List.of(1, 2), receivedCounts, "Snapshots after the second cell");

            clickCell(17, 3);
            assertEquals(List.of(1, 2, 3), receivedCounts, "Snapshots after the third cell");
        });
    }

    @Test
    @DisplayName("Clicks still toggle the board after an observer unsubscribes")
    void clickingStillChangesBoardAfterUnsubscribing() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int deadColor = renderedCellColor(2, 3);
            clickCell(2, 3);
            assertEquals(List.of(1), receivedCounts, "Snapshots before unsubscribing");
            assertNotEquals(deadColor, renderedCellColor(2, 3), "Cell after the first click");

            unsubscribe();
            clickCell(2, 3);

            assertEquals(List.of(1), receivedCounts, "Snapshots after unsubscribing");
            assertEquals(deadColor, renderedCellColor(2, 3), "Cell after the second click");
        });
    }

    private void unsubscribe() {
        universe.removeStatisticsListener(listener);
        subscribed = false;
    }

    private static void clearBoard() {
        JMenuItem clear = MenuSite.getMyMenuItem(universe, "Grid", "Clear");
        assertNotNull(clear, "Grid > Clear menu item");
        clear.doClick(0);
    }

    private static void clickCell(int column, int row) {
        universe.dispatchEvent(new MouseEvent(universe, MouseEvent.MOUSE_PRESSED,
                0L, 0, cellCenter(column), cellCenter(row), 1, false, MouseEvent.BUTTON1));
    }

    private static int renderedCellColor(int column, int row) {
        BufferedImage board = new BufferedImage(universe.getWidth(), universe.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = board.createGraphics();
        try {
            universe.paint(graphics);
            return board.getRGB(cellCenter(column), cellCenter(row));
        } finally {
            graphics.dispose();
        }
    }

    private static int cellCenter(int coordinate) {
        return coordinate * CELL_PX + CELL_PX / 2;
    }
}
