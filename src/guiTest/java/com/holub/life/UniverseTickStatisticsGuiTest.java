package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
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
class UniverseTickStatisticsGuiTest {

    private static final int CELL_PX = 8;
    private static final int AUTO_TICK_INTERVAL_MS = 60_000;
    private static JFrame frame;
    private static Universe universe;
    private static Clock clock;

    private StatisticsListener listener;

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
    void resetGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            MenuSelectionManager.defaultManager().clearSelectedPath();
            clearBoard();
        });
    }

    @AfterEach
    void cleanUpGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            try {
                if (listener != null) {
                    universe.removeStatisticsListener(listener);
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
    @DisplayName("One clock tick automatically publishes one statistics snapshot")
    void publishesExactlyOncePerClockTick() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(3, 4);
            clickCell(4, 4);
            clickCell(5, 4);
            List<GameStatistics> received = new ArrayList<>();
            register(received::add);

            clock.tick();

            assertEquals(1, received.size(), "Notification count after one Clock.tick()");
            assertNotNull(received.get(0), "Statistics snapshot");
        });
    }

    @Test
    @DisplayName("Tick publishes the next generation after an isolated cell dies")
    void publishesCountAfterIsolatedCellDies() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int deadColor = renderedCellColor(3, 4);
            clickCell(3, 4);
            assertNotEquals(deadColor, renderedCellColor(3, 4), "Cell must be alive before the tick");
            List<GameStatistics> received = new ArrayList<>();
            register(received::add);

            clock.tick();

            assertEquals(deadColor, renderedCellColor(3, 4), "Cell must be dead after the tick");
            assertEquals(1, received.size(), "Notification count after one Clock.tick()");
            assertEquals(0, received.get(0).getAliveCells(), "Published count after the cell dies");
        });
    }

    @Test
    @DisplayName("A tick on an empty board still publishes one zero-count snapshot")
    void publishesZeroOnceWhenBoardStaysEmpty() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<GameStatistics> received = new ArrayList<>();
            register(received::add);

            clock.tick();

            assertEquals(1, received.size(), "Notification count for an unchanged board");
            assertEquals(0, received.get(0).getAliveCells(), "Published count for an empty board");
        });
    }

    @Test
    @DisplayName("An active menu suppresses board advancement and statistics until selection is cleared")
    void skipsTickWhileMenuIsActive() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(3, 4);
            int liveColor = renderedCellColor(3, 4);
            List<GameStatistics> received = new ArrayList<>();
            register(received::add);
            MenuSelectionManager menus = MenuSelectionManager.defaultManager();
            try {
                menus.setSelectedPath(new MenuElement[] {
                        frame.getJMenuBar(), frame.getJMenuBar().getMenu(0) });

                clock.tick();

                assertEquals(liveColor, renderedCellColor(3, 4), "The selected menu must prevent advancement");
                assertEquals(0, received.size(), "A suppressed tick must not publish statistics");
            } finally {
                menus.clearSelectedPath();
            }

            clock.tick();

            assertEquals(1, received.size(), "The next permitted tick must publish once");
            assertEquals(0, received.get(0).getAliveCells(), "The isolated cell dies after selection is cleared");
        });
    }

    @Test
    @DisplayName("The manual menu and automatic timer both publish statistics")
    void publishesForManualMenuAndAutomaticTimer() throws Exception {
        BlockingQueue<Boolean> callbacksOnEdt = new LinkedBlockingQueue<>();
        SwingUtilities.invokeAndWait(() -> {
            register(statistics -> callbacksOnEdt.add(SwingUtilities.isEventDispatchThread()));
            JMenuItem tick = MenuSite.getMyMenuItem(clock, "Go", "Tick (Single Step)");
            assertNotNull(tick, "Go > Tick (Single Step) menu item");
            tick.doClick(0);
        });
        assertEquals(Boolean.TRUE, callbacksOnEdt.poll(), "Manual tick must notify on the EDT");
        assertEquals(0, callbacksOnEdt.size(), "Manual tick must notify only once");

        // The first timer tick is immediate. Its repeat is outside the five-second wait.
        SwingUtilities.invokeAndWait(() -> clock.startTicking(AUTO_TICK_INTERVAL_MS));
        assertEquals(Boolean.FALSE, callbacksOnEdt.poll(5, TimeUnit.SECONDS),
                "Automatic tick must notify off the EDT within five seconds");
    }

    @Test
    @DisplayName("The Halt menu cancels further automatic ticks after the current tick finishes")
    void haltStopsFurtherAutomaticTicks() throws Exception {
        BlockingQueue<GameStatistics> received = new LinkedBlockingQueue<>();
        CountDownLatch firstTickStarted = new CountDownLatch(1);
        CountDownLatch finishFirstTick = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            register(statistics -> {
                received.add(statistics);
                firstTickStarted.countDown();
                try {
                    assertTrue(finishFirstTick.await(5, TimeUnit.SECONDS), "The test must release the current tick");
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(exception);
                }
            });
            clock.startTicking(50);
        });
        try {
            assertTrue(firstTickStarted.await(5, TimeUnit.SECONDS), "The automatic timer must start");
            SwingUtilities.invokeAndWait(() -> {
                JMenuItem halt = MenuSite.getMyMenuItem(clock, "Go", "Halt");
                assertNotNull(halt, "Go > Halt menu item");
                halt.doClick(0);
            });
        } finally {
            finishFirstTick.countDown();
        }

        assertNotNull(received.poll(5, TimeUnit.SECONDS), "The current tick must publish once");
        assertNull(received.poll(250, TimeUnit.MILLISECONDS), "Halt must cancel subsequent timer ticks");
    }

    private void register(StatisticsListener observer) {
        universe.addStatisticsListener(observer);
        listener = observer;
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
