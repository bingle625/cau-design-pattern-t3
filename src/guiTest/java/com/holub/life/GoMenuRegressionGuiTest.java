package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
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

/** Pins down the original Go menu behavior that E4 must not change. */
@Execution(ExecutionMode.SAME_THREAD)
class GoMenuRegressionGuiTest {

    private static final int CELL_PX = 8;
    private static final long WAIT_MS = 5_000;
    private static final long QUIET_MS = 400;
    private static JFrame frame;
    private static Universe universe;
    private static Clock clock;

    private final AtomicInteger generations = new AtomicInteger();
    private StatisticsListener listener;

    @BeforeAll
    static void initializeGame() throws Exception {
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
            clickMenu(universe, "Grid", "Clear");
            generations.set(0);
            listener = statistics -> generations.incrementAndGet();
            universe.addStatisticsListener(listener);
        });
    }

    @AfterEach
    void cleanUpGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            universe.removeStatisticsListener(listener);
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
    @DisplayName("Go > Tick (Single Step) advances the board by exactly one generation")
    void tickAdvancesExactlyOneGeneration() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int deadColor = renderedCellColor(4, 3);
            clickCell(3, 4);
            clickCell(4, 4);
            clickCell(5, 4);
            generations.set(0);

            clickMenu(clock, "Go", "Tick (Single Step)");

            assertEquals(1, generations.get(), "Generations after one single step");
            assertNotEquals(deadColor, renderedCellColor(4, 3), "Blinker must turn vertical (top cell alive)");
            assertNotEquals(deadColor, renderedCellColor(4, 5), "Blinker must turn vertical (bottom cell alive)");
            assertEquals(deadColor, renderedCellColor(3, 4), "Blinker must turn vertical (left cell dead)");
            assertEquals(deadColor, renderedCellColor(5, 4), "Blinker must turn vertical (right cell dead)");
        });
    }

    @Test
    @DisplayName("Go > Tick (Single Step) does not start automatic progress")
    void tickDoesNotStartTheTimer() throws Exception {
        SwingUtilities.invokeAndWait(() -> clickMenu(clock, "Go", "Tick (Single Step)"));

        Thread.sleep(QUIET_MS);

        assertEquals(1, generations.get(), "Generations a while after one single step");
    }

    @Test
    @DisplayName("Go > Agonizing starts automatic progress")
    void agonizingStartsAutomaticProgress() throws Exception {
        assertSpeedItemKeepsAdvancing("Agonizing");
    }

    @Test
    @DisplayName("Go > Slow starts automatic progress")
    void slowStartsAutomaticProgress() throws Exception {
        assertSpeedItemKeepsAdvancing("Slow");
    }

    @Test
    @DisplayName("Go > Medium starts automatic progress")
    void mediumStartsAutomaticProgress() throws Exception {
        assertSpeedItemKeepsAdvancing("Medium");
    }

    @Test
    @DisplayName("Go > Fast starts automatic progress")
    void fastStartsAutomaticProgress() throws Exception {
        assertSpeedItemKeepsAdvancing("Fast");
    }

    @Test
    @DisplayName("Go > Halt stops automatic progress until a speed is chosen again")
    void haltStopsAutomaticProgress() throws Exception {
        SwingUtilities.invokeAndWait(() -> clickMenu(clock, "Go", "Fast"));
        assertTrue(awaitGenerations(3), "Fast must advance several generations before Halt");

        SwingUtilities.invokeAndWait(() -> clickMenu(clock, "Go", "Halt"));
        // A tick that was already running when Halt was chosen may still finish.
        Thread.sleep(QUIET_MS);
        int afterHalt = generations.get();
        Thread.sleep(QUIET_MS);

        assertEquals(afterHalt, generations.get(), "Generations must not advance after Halt");

        SwingUtilities.invokeAndWait(() -> clickMenu(clock, "Go", "Fast"));
        assertTrue(awaitGenerations(afterHalt + 3), "Choosing a speed again must resume progress");
    }

    private void assertSpeedItemKeepsAdvancing(String item) throws Exception {
        SwingUtilities.invokeAndWait(() -> clickMenu(clock, "Go", item));

        // The first timer tick is immediate, so a third one proves the timer repeats.
        assertTrue(awaitGenerations(3), "Go > " + item + " must keep advancing generations");
    }

    private boolean awaitGenerations(int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        while (generations.get() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        return generations.get() >= expected;
    }

    private static void clickMenu(Object requester, String menu, String item) {
        JMenuItem menuItem = MenuSite.getMyMenuItem(requester, menu, item);
        assertNotNull(menuItem, menu + " > " + item + " menu item");
        menuItem.doClick(0);
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
