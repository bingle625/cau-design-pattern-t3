package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.event.MouseEvent;
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
class UniverseStatisticsGuiTest {

    private static final int CELL_PX = 8;
    private static JFrame frame;
    private static Universe universe;

    private final List<StatisticsListener> registeredListeners = new ArrayList<>();

    @BeforeAll
    static void initializeUniverse() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame();
            MenuSite.establish(frame);
            universe = Universe.instance();
            frame.add(universe);
            frame.pack();
        });
    }

    @BeforeEach
    void resetBoard() throws Exception {
        SwingUtilities.invokeAndWait(UniverseStatisticsGuiTest::clearBoard);
    }

    @AfterEach
    void cleanUpTestState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                registeredListeners.forEach(universe::removeStatisticsListener);
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
    @DisplayName("One publication delivers one non-null snapshot to a registered observer")
    void deliversOneSnapshotToRegisteredObserver() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<GameStatistics> received = new ArrayList<>();
            register(received::add);

            universe.publishStatistics();

            assertEquals(1, received.size(), "Observer notification count");
            assertNotNull(received.get(0), "Published statistics snapshot");
        });
    }

    @Test
    @DisplayName("Two observers each receive the same snapshot once")
    void deliversSameSnapshotToBothObservers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<GameStatistics> first = new ArrayList<>();
            List<GameStatistics> second = new ArrayList<>();
            register(first::add);
            register(second::add);

            universe.publishStatistics();

            assertEquals(1, first.size(), "First observer notification count");
            assertEquals(1, second.size(), "Second observer notification count");
            assertNotNull(first.get(0), "Published statistics snapshot");
            assertSame(first.get(0), second.get(0), "Snapshot instance shared by both observers");
        });
    }

    @Test
    @DisplayName("An observer receives no further notifications after removal")
    void stopsDeliveryAfterObserverRemoval() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<GameStatistics> received = new ArrayList<>();
            StatisticsListener listener = received::add;
            register(listener);

            universe.publishStatistics();
            assertEquals(1, received.size(), "Notification count before removal");

            universe.removeStatisticsListener(listener);
            registeredListeners.remove(listener);
            universe.publishStatistics();

            assertEquals(1, received.size(), "Notification count after removal");
        });
    }

    @Test
    @DisplayName("The published live count matches the cells activated on the board")
    void publishesAliveCountFromBoardClicks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(2, 3);
            clickCell(10, 11);
            clickCell(17, 3);
            List<GameStatistics> received = new ArrayList<>();
            register(received::add);

            universe.publishStatistics();

            assertEquals(1, received.size(), "Observer notification count");
            assertNotNull(received.get(0), "Published statistics snapshot");
            assertEquals(3, received.get(0).getAliveCells(), "Live cells after three board clicks");
        });
    }

    private void register(StatisticsListener listener) {
        universe.addStatisticsListener(listener);
        registeredListeners.add(listener);
    }

    private static void clearBoard() {
        JMenuItem clear = MenuSite.getMyMenuItem(universe, "Grid", "Clear");
        assertNotNull(clear, "Grid > Clear menu item");
        clear.doClick(0);
    }

    private static void clickCell(int column, int row) {
        int x = column * CELL_PX + CELL_PX / 2;
        int y = row * CELL_PX + CELL_PX / 2;
        universe.dispatchEvent(new MouseEvent(universe, MouseEvent.MOUSE_PRESSED,
                0L, 0, x, y, 1, false, MouseEvent.BUTTON1));
    }
}
