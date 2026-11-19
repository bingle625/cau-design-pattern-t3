package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
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
class UniverseLoadFailureGuiTest {

    private static final int CELL_PX = 8;
    private static final Set<BoardCell> THREE_CELLS = Set.of(
            new BoardCell(2, 3), new BoardCell(10, 11), new BoardCell(17, 3));
    private static JFrame frame;
    private static Universe universe;

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
    @AfterEach
    void resetBoard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Clock.instance().stop();
            gridMenuItem("Clear").doClick(0);
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
    @DisplayName("A failed read preserves the live cells and the observer's last count")
    void failedReadPreservesBoardAndLastStatistics(@TempDir Path tempDirectory) throws Exception {
        Path damagedSave = Files.createFile(tempDirectory.resolve("damaged.life"));
        SwingUtilities.invokeAndWait(() -> {
            BufferedImage empty = renderBoard();
            List<Integer> received = new ArrayList<>();
            StatisticsListener listener = statistics -> received.add(statistics.getAliveCells());
            universe.addStatisticsListener(listener);
            try {
                THREE_CELLS.forEach(UniverseLoadFailureGuiTest::clickCell);
                Set<BoardCell> beforeLoad = renderedLiveCells(empty);
                assertEquals(THREE_CELLS, beforeLoad, "Live cell positions before Load");
                assertEquals(List.of(1, 2, 3), received, "Observer counts from the three clicks");

                new FailedLoadDialogs(damagedSave).run();

                assertEquals(beforeLoad, renderedLiveCells(empty), "Live cell positions after failed Load");
                assertEquals(List.of(1, 2, 3), received, "Failed Load must not publish another snapshot");
            } finally {
                universe.removeStatisticsListener(listener);
            }
        });
    }

    @Test
    @DisplayName("The damaged save can be deleted immediately after Load fails")
    void failedReadReleasesFileForImmediateDeletion(@TempDir Path tempDirectory) throws Exception {
        Path damagedSave = Files.createFile(tempDirectory.resolve("damaged.life"));
        SwingUtilities.invokeAndWait(() -> new FailedLoadDialogs(damagedSave).run());

        Files.delete(damagedSave);
        assertTrue(Files.notExists(damagedSave), "The damaged save must be gone before the test JVM exits");
    }

    private static JMenuItem gridMenuItem(String name) {
        JMenuItem item = MenuSite.getMyMenuItem(universe, "Grid", name);
        assertNotNull(item, "Grid > " + name + " menu item");
        return item;
    }

    private static void clickCell(BoardCell cell) {
        universe.dispatchEvent(new MouseEvent(universe, MouseEvent.MOUSE_PRESSED,
                0L, 0, cellCenter(cell.column()), cellCenter(cell.row()), 1, false, MouseEvent.BUTTON1));
    }

    private static BufferedImage renderBoard() {
        BufferedImage image = new BufferedImage(universe.getWidth(), universe.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            universe.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static Set<BoardCell> renderedLiveCells(BufferedImage empty) {
        BufferedImage current = renderBoard();
        Set<BoardCell> liveCells = new HashSet<>();
        for (int row = 0; row < empty.getHeight() / CELL_PX; row++) {
            for (int column = 0; column < empty.getWidth() / CELL_PX; column++) {
                int x = cellCenter(column);
                int y = cellCenter(row);
                if (empty.getRGB(x, y) != current.getRGB(x, y)) {
                    liveCells.add(new BoardCell(column, row));
                }
            }
        }
        return liveCells;
    }

    private static int cellCenter(int coordinate) {
        return coordinate * CELL_PX + CELL_PX / 2;
    }

    private record BoardCell(int column, int row) {}

    private static final class FailedLoadDialogs {
        private final Path damagedSave;
        private final Set<Window> existingWindows = Set.of(Window.getWindows());
        private final Timer timer = new Timer(25, event -> handleDialogs());
        private long deadline;
        private boolean fileApproved;
        private boolean timedOut;
        private JOptionPane errorPane;

        private FailedLoadDialogs(Path damagedSave) {
            this.damagedSave = damagedSave;
        }

        private void run() {
            deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            timer.start();
            try {
                // The chooser and error dialog run nested EDT event loops.
                gridMenuItem("Load").doClick(0);
            } finally {
                timer.stop();
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog && !existingWindows.contains(window)) {
                        window.dispose();
                    }
                }
            }

            assertFalse(timedOut, "Timed out handling the Load dialogs");
            assertTrue(fileApproved, "The Load file chooser did not accept the damaged save");
            assertNotNull(errorPane, "Expected a read error dialog after approving the damaged save");
            assertEquals(JOptionPane.ERROR_MESSAGE, errorPane.getMessageType());
            assertEquals("Read Failed!", errorPane.getMessage());
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
                if (chooser != null && !fileApproved) {
                    fileApproved = true;
                    chooser.setSelectedFile(damagedSave.toFile());
                    chooser.approveSelection();
                    continue;
                }
                JOptionPane pane = findComponent(dialog, JOptionPane.class);
                if (pane != null) {
                    errorPane = pane;
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
