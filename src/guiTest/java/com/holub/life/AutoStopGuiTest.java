package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/**
 * E4 auto-stop checked against the real clock, universe and menus.
 * Generations are counted through the statistics notification that the
 * universe publishes at the end of every tick.
 */
@Execution(ExecutionMode.SAME_THREAD)
class AutoStopGuiTest {

    private static final int CELL_PX = 8;
    private static final long WAIT_MS = 5_000;
    private static final long QUIET_MS = 400;
    private static JFrame frame;
    private static Universe universe;
    private static Clock clock;
    private static AutoStopMenu autoStop;

    private final AtomicInteger generations = new AtomicInteger();
    private final AtomicInteger lastAliveCells = new AtomicInteger();
    private StatisticsListener listener;

    @BeforeAll
    static void initializeGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame();
            MenuSite.establish(frame);
            universe = Universe.instance();
            clock = Clock.instance();
            autoStop = AutoStopMenu.install(clock, universe);
            frame.add(universe);
            frame.pack();
        });
    }

    @BeforeEach
    void resetGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            autoStop.turnOff();
            MenuSelectionManager.defaultManager().clearSelectedPath();
            clickMenu(universe, "Grid", "Clear");
            listener = statistics -> {
                lastAliveCells.set(statistics.getAliveCells());
                generations.incrementAndGet();
            };
            universe.addStatisticsListener(listener);
        });
    }

    @AfterEach
    void cleanUpGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            autoStop.turnOff();
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
    @DisplayName("After N Generations runs a Blinker for exactly 5 generations and then stops by itself")
    void runsExactlyNGenerationsThenStops() throws Exception {
        int[] deadColor = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            deadColor[0] = renderedCellColor(4, 3);
            placeHorizontalBlinker();
            generations.set(0);

            autoStop.runGenerations(5);
        });

        assertTrue(awaitGenerations(5), "Five generations must run");
        Thread.sleep(QUIET_MS);
        assertEquals(5, generations.get(), "The run must stop by itself after the fifth generation");
        SwingUtilities.invokeAndWait(() -> {
            // An odd number of generations leaves the Blinker vertical.
            assertNotEquals(deadColor[0], renderedCellColor(4, 3), "Top cell of the vertical Blinker");
            assertEquals(deadColor[0], renderedCellColor(3, 4), "Left cell of the horizontal Blinker");
        });
    }

    @Test
    @DisplayName("When Extinct stops the run in the generation where the last cell dies")
    void stopsWhenTheBoardBecomesEmpty() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(10, 10);
            generations.set(0);
            clickMenu(autoStop, AutoStopMenu.MENU, AutoStopMenu.WHEN_EXTINCT);

            clickMenu(clock, "Go", "Fast");
        });

        assertTrue(awaitGenerations(1), "The lone cell must die in the first generation");
        Thread.sleep(QUIET_MS);
        assertEquals(1, generations.get(), "The run must stop once the board is empty");
        assertEquals(0, lastAliveCells.get(), "No cell is alive when the run stops");
    }

    @Test
    @DisplayName("When Extinct lets the run continue while cells are alive")
    void keepsRunningWhileCellsAreAlive() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            placeHorizontalBlinker();
            generations.set(0);
            clickMenu(autoStop, AutoStopMenu.MENU, AutoStopMenu.WHEN_EXTINCT);

            clickMenu(clock, "Go", "Fast");
        });

        assertTrue(awaitGenerations(10), "A Blinker never dies, so the run must go on");
    }

    @Test
    @DisplayName("At Population stops the run when the live-cell count reaches the threshold")
    void stopsWhenPopulationReachesThreshold() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // A row of five cells grows into nine cells in one generation.
            for (int column = 10; column <= 14; column++) {
                clickCell(column, 10);
            }
            generations.set(0);
            autoStop.stopAtPopulation(9);

            clickMenu(clock, "Go", "Fast");
        });

        assertTrue(awaitGenerations(1), "The first generation must run");
        Thread.sleep(QUIET_MS);
        assertEquals(1, generations.get(), "The run must stop as soon as nine cells are alive");
        assertEquals(9, lastAliveCells.get(), "Live cells when the run stops");
    }

    @Test
    @DisplayName("Off removes the condition so the run continues past the point where it would have stopped")
    void offRemovesTheCondition() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(10, 10);
            generations.set(0);
            clickMenu(autoStop, AutoStopMenu.MENU, AutoStopMenu.WHEN_EXTINCT);
            clickMenu(autoStop, AutoStopMenu.MENU, AutoStopMenu.OFF);

            clickMenu(clock, "Go", "Fast");
        });

        assertTrue(awaitGenerations(10), "Without a condition the run goes on over an empty board");
    }

    @Test
    @DisplayName("Halt keeps the condition, and single steps count toward the remaining generations")
    void haltKeepsConditionAndSingleStepsCount() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            placeHorizontalBlinker();
            generations.set(0);
            autoStop.runGenerations(4);
            clickMenu(clock, "Go", "Halt");
        });
        // The timer fires once immediately, so zero or one generation may have run before Halt.
        Thread.sleep(QUIET_MS);
        int beforeResuming = generations.get();
        assertTrue(beforeResuming < 4, "Halt must interrupt the run before it finishes");

        for (int generation = beforeResuming + 1; generation <= 4; generation++) {
            SwingUtilities.invokeAndWait(() ->
                    assertTrue(isChecked(AutoStopMenu.AFTER_GENERATIONS), "Condition must survive Halt"));
            SwingUtilities.invokeAndWait(() -> clickMenu(clock, "Go", "Tick (Single Step)"));
        }

        SwingUtilities.invokeAndWait(() ->
                assertFalse(isChecked(AutoStopMenu.AFTER_GENERATIONS),
                        "Condition must be met by the fourth generation overall"));
    }

    @Test
    @DisplayName("The menu marks the chosen condition and clears the mark when it is turned off")
    void menuMarksTheChosenCondition() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(isChecked(AutoStopMenu.WHEN_EXTINCT), "Nothing is marked at first");

            clickMenu(autoStop, AutoStopMenu.MENU, AutoStopMenu.WHEN_EXTINCT);
            assertTrue(isChecked(AutoStopMenu.WHEN_EXTINCT), "The chosen item is marked");
            assertFalse(isChecked(AutoStopMenu.AT_POPULATION), "Other items stay unmarked");

            autoStop.stopAtPopulation(50);
            assertTrue(isChecked(AutoStopMenu.AT_POPULATION), "A newer choice takes the mark");
            assertFalse(isChecked(AutoStopMenu.WHEN_EXTINCT), "The older choice loses the mark");

            clickMenu(autoStop, AutoStopMenu.MENU, AutoStopMenu.OFF);
            assertFalse(isChecked(AutoStopMenu.AT_POPULATION), "Off clears the mark");
        });
    }

    @Test
    @DisplayName("The mark is cleared after the condition has stopped the run")
    void markIsClearedAfterStopping() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            generations.set(0);
            autoStop.runGenerations(2);
            assertTrue(isChecked(AutoStopMenu.AFTER_GENERATIONS), "Marked while the run is in progress");
        });

        assertTrue(awaitGenerations(2), "Two generations must run");
        Thread.sleep(QUIET_MS);
        SwingUtilities.invokeAndWait(() ->
                assertFalse(isChecked(AutoStopMenu.AFTER_GENERATIONS), "Unmarked once the run has stopped"));
    }

    @Test
    @DisplayName("With no condition set, the Go menu behaves as in the original game")
    void goMenuIsUnchangedWithoutCondition() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            generations.set(0);
            clickMenu(clock, "Go", "Tick (Single Step)");
            assertEquals(1, generations.get(), "One single step is one generation");

            clickMenu(clock, "Go", "Fast");
        });
        assertTrue(awaitGenerations(10), "Fast keeps running");

        SwingUtilities.invokeAndWait(() -> clickMenu(clock, "Go", "Halt"));
        Thread.sleep(QUIET_MS);
        int afterHalt = generations.get();
        Thread.sleep(QUIET_MS);
        assertEquals(afterHalt, generations.get(), "Halt stops the run");
    }

    private static void placeHorizontalBlinker() {
        clickCell(3, 4);
        clickCell(4, 4);
        clickCell(5, 4);
    }

    private boolean awaitGenerations(int expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        while (generations.get() < expected && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        return generations.get() >= expected;
    }

    private static boolean isChecked(String item) {
        return !menuItem(autoStop, AutoStopMenu.MENU, item).getText().equals(item);
    }

    private static void clickMenu(Object requester, String menu, String item) {
        menuItem(requester, menu, item).doClick(0);
    }

    private static JMenuItem menuItem(Object requester, String menu, String item) {
        JMenuItem menuItem = MenuSite.getMyMenuItem(requester, menu, item);
        assertNotNull(menuItem, menu + " > " + item + " menu item");
        return menuItem;
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
