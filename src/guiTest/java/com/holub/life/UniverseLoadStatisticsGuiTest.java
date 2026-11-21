package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.holub.ui.MenuSite;

@Execution(ExecutionMode.SAME_THREAD)
class UniverseLoadStatisticsGuiTest {

    private static final int CELL_PX = 8;
    private static final BoardCell FIRST = new BoardCell(2, 3);
    private static final BoardCell SECOND = new BoardCell(10, 11);
    private static final BoardCell THIRD = new BoardCell(17, 3);
    private static final BoardCell OTHER = new BoardCell(25, 18);
    private static final Set<BoardCell> THREE_CELLS = Set.of(FIRST, SECOND, THIRD);

    private static JFrame frame;
    private static Universe universe;
    private static Clock clock;

    @TempDir
    Path tempDirectory;

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
            gridMenuItem("Clear").doClick(0);
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
                gridMenuItem("Clear").doClick(0);
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
    @DisplayName("Loading a populated file publishes the restored count exactly once")
    void publishesRestoredCountOnce() throws Exception {
        Path saved = saveBoard("populated.life", THREE_CELLS);
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage empty = renderBoard();
            clickCell(OTHER);
            assertBoard(empty, Set.of(OTHER));
            subscribe();

            assertNull(load(saved), "Unexpected Load error dialog");

            assertEquals(List.of(3), receivedCounts, "Only the restored count should be published");
            assertBoard(empty, THREE_CELLS);
        });
    }

    @Test
    @DisplayName("Loading an empty file clears the board and publishes zero once")
    void publishesZeroOnceAfterLoadingEmptyBoard() throws Exception {
        Path saved = saveBoard("empty.life", Set.of());
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage empty = renderBoard();
            clickCell(OTHER);
            assertBoard(empty, Set.of(OTHER));
            subscribe();

            assertNull(load(saved), "Unexpected Load error dialog");

            assertEquals(List.of(0), receivedCounts, "Snapshots after loading an empty file");
            assertBoard(empty, Set.of());
        });
    }

    @Test
    @DisplayName("Canceling the chooser preserves the board and publishes no statistics")
    void cancelsLoadWithoutNotifyingOrChangingBoard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage empty = renderBoard();
            THREE_CELLS.forEach(UniverseLoadStatisticsGuiTest::clickCell);
            assertBoard(empty, THREE_CELLS);
            subscribe();

            assertEquals("Read Failed!", load(null), "Existing cancellation dialog");

            assertEquals(List.of(), receivedCounts, "Snapshots after canceling the chooser");
            assertBoard(empty, THREE_CELLS);
        });
    }

    @Test
    @DisplayName("Loading after unsubscription still restores the board without notifying the observer")
    void restoresBoardWithoutNotifyingUnsubscribedObserver() throws Exception {
        Path firstSave = saveBoard("first.life", Set.of(FIRST, SECOND));
        Path secondSave = saveBoard("second.life", Set.of(OTHER));
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage empty = renderBoard();
            subscribe();
            assertNull(load(firstSave), "Unexpected first Load error dialog");
            assertEquals(List.of(2), receivedCounts, "Snapshots before unsubscribing");
            assertBoard(empty, Set.of(FIRST, SECOND));

            unsubscribe();
            assertNull(load(secondSave), "Unexpected second Load error dialog");

            assertEquals(List.of(2), receivedCounts, "Snapshots after unsubscribing");
            assertBoard(empty, Set.of(OTHER));
        });
    }

    @Test
    @DisplayName("A successful file Load clears the completed generation history")
    void clearsHistoryAfterSuccessfulLoad() throws Exception {
        Path saved = saveBoard("history.life", THREE_CELLS);
        SwingUtilities.invokeAndWait(() -> {
            clock.tick();
            assertEquals(1, universe.generationHistorySize());

            assertNull(load(saved));

            assertEquals(0, universe.generationHistorySize());
        });
    }

    @Test
    @DisplayName("Canceling Load preserves completed generation history")
    void keepsHistoryAfterCancelingLoad() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.tick();
            assertEquals(1, universe.generationHistorySize());

            assertEquals("Read Failed!", load(null));

            assertEquals(1, universe.generationHistorySize());
        });
    }

    @Test
    @DisplayName("A failed file Load preserves completed generation history")
    void keepsHistoryAfterFailedLoad() throws Exception {
        Path damaged = Files.createFile(tempDirectory.resolve("history-damaged.life"));
        SwingUtilities.invokeAndWait(() -> {
            clock.tick();
            assertEquals(1, universe.generationHistorySize());

            assertEquals("Read Failed!", load(damaged));

            assertEquals(1, universe.generationHistorySize());
        });
    }

    @Test
    @DisplayName("Store still writes a file and preserves the completed generation history")
    void keepsHistoryAfterStore() throws Exception {
        Path saved = tempDirectory.resolve("history-store.life");
        SwingUtilities.invokeAndWait(() -> {
            clock.tick();
            assertEquals(1, universe.generationHistorySize());

            assertNull(new LoadDialogs(saved).run(gridMenuItem("Store")));

            assertEquals(1, universe.generationHistorySize());
        });
        assertTrue(Files.exists(saved));
    }

    private Path saveBoard(String name, Set<BoardCell> liveCells) throws IOException {
        Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
        Rectangle surface = new Rectangle(0, 0, board.widthInCells(), board.widthInCells());
        for (BoardCell cell : liveCells) {
            board.userClicked(new Point(cell.column(), cell.row()), surface);
        }
        Path file = tempDirectory.resolve(name);
        try (OutputStream output = Files.newOutputStream(file)) {
            board.createMemento().flush(output);
        }
        return file;
    }

    private void subscribe() {
        universe.addStatisticsListener(listener);
        subscribed = true;
    }

    private void unsubscribe() {
        universe.removeStatisticsListener(listener);
        subscribed = false;
    }

    private static JMenuItem gridMenuItem(String name) {
        JMenuItem item = MenuSite.getMyMenuItem(universe, "Grid", name);
        assertNotNull(item, "Grid > " + name + " menu item");
        return item;
    }

    private static Object load(Path file) {
        return new LoadDialogs(file).run(gridMenuItem("Load"));
    }

    private static void clickCell(BoardCell cell) {
        universe.dispatchEvent(new MouseEvent(universe, MouseEvent.MOUSE_PRESSED,
                0L, 0, cellCenter(cell.column()), cellCenter(cell.row()), 1, false, MouseEvent.BUTTON1));
    }

    private static BufferedImage renderBoard() {
        BufferedImage board = new BufferedImage(universe.getWidth(), universe.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = board.createGraphics();
        try {
            universe.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return board;
    }

    private static void assertBoard(BufferedImage empty, Set<BoardCell> liveCells) {
        BufferedImage current = renderBoard();
        for (int row = 0; row < empty.getHeight() / CELL_PX; row++) {
            for (int column = 0; column < empty.getWidth() / CELL_PX; column++) {
                BoardCell cell = new BoardCell(column, row);
                int x = cellCenter(column);
                int y = cellCenter(row);
                boolean alive = empty.getRGB(x, y) != current.getRGB(x, y);
                assertEquals(liveCells.contains(cell), alive, () -> "Unexpected state at " + cell);
            }
        }
    }

    private static int cellCenter(int coordinate) {
        return coordinate * CELL_PX + CELL_PX / 2;
    }

    private record BoardCell(int column, int row) {}

    private static final class LoadDialogs {
        private final Path file;
        private final Set<Window> existingWindows = Set.of(Window.getWindows());
        private final Timer timer = new Timer(25, event -> handleDialogs());
        private long deadline;
        private boolean chooserHandled;
        private boolean timedOut;
        private Object message;

        private LoadDialogs(Path file) {
            this.file = file;
        }

        private Object run(JMenuItem load) {
            deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            timer.start();
            try {
                // The modal chooser and error dialog each run a nested EDT event loop.
                load.doClick(0);
            } finally {
                timer.stop();
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog && !existingWindows.contains(window)) {
                        window.dispose();
                    }
                }
            }
            assertFalse(timedOut, "Timed out handling the Load dialogs");
            assertTrue(chooserHandled, "The Load file chooser did not open");
            return message;
        }

        private void handleDialogs() {
            for (Window window : Window.getWindows()) {
                if (!(window instanceof JDialog dialog) || !dialog.isShowing()
                        || existingWindows.contains(dialog)) {
                    continue;
                }
                if (System.nanoTime() - deadline >= 0) {
                    timedOut = true;
                    dialog.dispose();
                    continue;
                }
                JFileChooser chooser = findComponent(dialog, JFileChooser.class);
                if (chooser != null && !chooserHandled) {
                    chooserHandled = true;
                    if (file == null) {
                        chooser.cancelSelection();
                    } else {
                        chooser.setSelectedFile(file.toFile());
                        chooser.approveSelection();
                    }
                    continue;
                }
                JOptionPane pane = findComponent(dialog, JOptionPane.class);
                if (pane != null) {
                    message = pane.getMessage();
                    pane.setValue(JOptionPane.OK_OPTION);
                }
            }
        }

        private static <T extends Component> T findComponent(Component root, Class<T> type) {
            if (type.isInstance(root)) {
                return type.cast(root);
            }
            if (root instanceof Container container) {
                for (Component child : container.getComponents()) {
                    T found = findComponent(child, type);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }
    }
}
