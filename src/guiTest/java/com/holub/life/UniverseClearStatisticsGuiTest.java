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
class UniverseClearStatisticsGuiTest {

    private static final int CELL_PX = 8;
    private static final List<BoardCell> CELLS = List.of(
            new BoardCell(2, 3), new BoardCell(10, 11), new BoardCell(17, 3));
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
    void resetBoard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            clearBoard();
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
    @DisplayName("Clearing a populated board publishes zero exactly once")
    void publishesZeroOnceAfterClearingPopulatedBoard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CELLS.forEach(UniverseClearStatisticsGuiTest::clickCell);
            subscribe();

            clearBoard();

            assertEquals(List.of(0), receivedCounts, "Snapshots after clearing three live cells");
        });
    }

    @Test
    @DisplayName("Clearing an already empty board still publishes zero once")
    void publishesZeroOnceAfterClearingEmptyBoard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            subscribe();

            clearBoard();

            assertEquals(List.of(0), receivedCounts, "Snapshots after clearing an empty board");
        });
    }

    @Test
    @DisplayName("Clearing after an observer unsubscribes sends it no additional snapshot")
    void stopsDeliveryAfterObserverUnsubscribes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            subscribe();
            clearBoard();
            assertEquals(List.of(0), receivedCounts, "Snapshots before unsubscribing");

            unsubscribe();
            clearBoard();

            assertEquals(List.of(0), receivedCounts, "Snapshots after unsubscribing");
        });
    }

    @Test
    @DisplayName("Clear resets all activated cells on the board and publishes zero")
    void clearsActualBoardAndPublishesZero() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage empty = renderBoard();
            CELLS.forEach(UniverseClearStatisticsGuiTest::clickCell);
            BufferedImage populated = renderBoard();
            for (BoardCell cell : CELLS) {
                assertNotEquals(cellColor(empty, cell), cellColor(populated, cell),
                        "Cell must be alive before clearing: " + cell);
            }
            subscribe();

            clearBoard();

            BufferedImage cleared = renderBoard();
            for (BoardCell cell : CELLS) {
                assertEquals(cellColor(empty, cell), cellColor(cleared, cell),
                        "Cell must be dead after clearing: " + cell);
            }
            assertEquals(List.of(0), receivedCounts, "Snapshots after clearing the rendered board");
        });
    }

    private void subscribe() {
        universe.addStatisticsListener(listener);
        subscribed = true;
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

    private static void clickCell(BoardCell cell) {
        universe.dispatchEvent(new MouseEvent(universe, MouseEvent.MOUSE_PRESSED,
                0L, 0, cellCenter(cell.column()), cellCenter(cell.row()), 1, false, MouseEvent.BUTTON1));
    }

    private static BufferedImage renderBoard() {
        BufferedImage board = new BufferedImage(universe.getWidth(), universe.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = board.createGraphics();
        try {
            universe.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return board;
    }

    private static int cellColor(BufferedImage board, BoardCell cell) {
        return board.getRGB(cellCenter(cell.column()), cellCenter(cell.row()));
    }

    private static int cellCenter(int coordinate) {
        return coordinate * CELL_PX + CELL_PX / 2;
    }

    private record BoardCell(int column, int row) {}
}
