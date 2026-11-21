package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
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
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.holub.ui.MenuSite;

@Timeout(30)
@Execution(ExecutionMode.SAME_THREAD)
class LifeStatisticsIntegrationGuiTest {

    private static GameWindow game;

    @BeforeAll
    static void startGame() throws Exception {
        game = GameWindow.open();
    }

    @BeforeEach
    @AfterEach
    void resetGame() throws Exception {
        SwingUtilities.invokeAndWait(game::reset);
    }

    @AfterAll
    static void disposeGame() throws Exception {
        if (game != null) {
            SwingUtilities.invokeAndWait(game.frame::dispose);
        }
    }

    @Test
    @DisplayName("Life opens with a full-size board and its statistics panel below it")
    void displaysBoardAndInitialStatisticsInLifeWindow() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(game.frame.isShowing(), "The Life frame should be visible");
            assertSame(Universe.instance(), game.board, "The CENTER component should be the game board");
            assertTrue(game.board.isShowing());
            assertTrue(game.panel.isShowing());
            assertTrue(game.label.isShowing());
            assertEquals(new Dimension(512, 512), game.board.getSize(), "Board size in the Life window");
            assertTrue(game.panel.getY() >= game.board.getY() + game.board.getHeight(),
                    "The SOUTH panel must not overlap the game board");
            assertEquals("Grid", game.frame.getJMenuBar().getMenu(0).getName());
            assertEquals("Go", game.frame.getJMenuBar().getMenu(1).getName());
            assertEquals("Alive Cells: 0", game.initialLabelText, "The label before any test resets the board");
        });
    }

    @Test
    @DisplayName("Clicking three cells updates the visible count after each click")
    void updatesStatisticsAfterBoardClicks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            game.clickCell(2, 3);
            game.assertAliveCells(1);
            game.clickCell(10, 11);
            game.assertAliveCells(2);
            game.clickCell(17, 3);
            game.assertAliveCells(3);
        });
    }

    @Test
    @DisplayName("The manual Tick menu removes an isolated cell and updates the visible count")
    void updatesStatisticsAfterManualTick() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int deadColor = game.renderedCellColor(3, 4);
            game.clickCell(3, 4);
            game.assertAliveCells(1);
            assertNotEquals(deadColor, game.renderedCellColor(3, 4), "The cell must be alive before Tick");

            GameWindow.menuItem(Clock.instance(), "Go", "Tick (Single Step)").doClick(0);

            game.assertAliveCells(0);
            assertEquals(deadColor, game.renderedCellColor(3, 4), "The isolated cell must die after Tick");
        });
    }

    @Test
    @DisplayName("Switching to Seeds updates the board and the visible count on the next tick")
    void updatesStatisticsAfterChangingRule() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            int deadColor = game.renderedCellColor(7, 7);
            game.clickCell(7, 7);
            game.clickCell(8, 7);
            game.clickCell(7, 8);
            game.clickCell(8, 8);
            JMenuItem tick = GameWindow.menuItem(Clock.instance(), "Go", "Tick (Single Step)");

            tick.doClick(0);
            game.assertAliveCells(4);
            GameWindow.menuItem(game.board, "Rule", "Seeds").doClick(0);
            game.assertAliveCells(4);
            assertNotEquals(deadColor, game.renderedCellColor(7, 7));

            tick.doClick(0);
            game.assertAliveCells(8);
            assertEquals(deadColor, game.renderedCellColor(7, 7));
            assertNotEquals(deadColor, game.renderedCellColor(8, 6));
        });
    }

    @Test
    @DisplayName("The Clear menu resets the visible count to zero")
    void updatesStatisticsAfterClear() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            game.clickCell(2, 3);
            game.clickCell(10, 11);
            game.assertAliveCells(2);

            GameWindow.menuItem(game.board, "Grid", "Clear").doClick(0);

            game.assertAliveCells(0);
        });
    }

    @Test
    @DisplayName("Loading a saved board updates the visible count to the restored value")
    void updatesStatisticsAfterLoad(@TempDir Path temporaryDirectory) throws Exception {
        Path saved = writeSavedBoard(temporaryDirectory.resolve("three-cells.life"));
        SwingUtilities.invokeAndWait(() -> {
            game.clickCell(3, 4);
            game.assertAliveCells(1);

            new LoadDialog(saved).run(GameWindow.menuItem(game.board, "Grid", "Load"));

            game.assertAliveCells(3);
        });
    }

    private static Path writeSavedBoard(Path file) throws IOException {
        Cell board = new Neighborhood(8, new Neighborhood(8, new Resident()));
        Rectangle surface = new Rectangle(0, 0, board.widthInCells(), board.widthInCells());
        board.userClicked(new Point(2, 3), surface);
        board.userClicked(new Point(10, 11), surface);
        board.userClicked(new Point(17, 3), surface);
        try (OutputStream output = Files.newOutputStream(file)) {
            board.createMemento().flush(output);
        }
        return file;
    }

    static final class GameWindow {
        private static final int CELL_PX = 8;
        final Life frame;
        final Universe board;
        final StatisticsPanel panel;
        final String initialLabelText;
        private final JLabel label;

        private GameWindow(Life frame) {
            this.frame = frame;
            BorderLayout layout = assertInstanceOf(BorderLayout.class, frame.getContentPane().getLayout());
            board = assertInstanceOf(Universe.class, layout.getLayoutComponent(BorderLayout.CENTER));
            panel = assertInstanceOf(StatisticsPanel.class, layout.getLayoutComponent(BorderLayout.SOUTH));
            assertEquals(1, panel.getComponentCount());
            label = assertInstanceOf(JLabel.class, panel.getComponent(0));
            initialLabelText = label.getText();
        }

        static GameWindow open() throws Exception {
            Life.main(new String[0]);
            FutureTask<GameWindow> findFrame = new FutureTask<>(() -> {
                Life frame = Arrays.stream(Frame.getFrames())
                        .filter(Life.class::isInstance)
                        .map(Life.class::cast)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Life.main() did not create a frame"));
                try {
                    assertTrue(frame.isShowing(), "Life.main() did not show the frame");
                    return new GameWindow(frame);
                } catch (RuntimeException | AssertionError failure) {
                    frame.dispose();
                    throw failure;
                }
            });
            SwingUtilities.invokeAndWait(findFrame);
            return findFrame.get();
        }

        void reset() {
            Clock.instance().stop();
            MenuSelectionManager.defaultManager().clearSelectedPath();
            menuItem(board, "Rule", "Conway").doClick(0);
            menuItem(board, "Grid", "Clear").doClick(0);
        }

        void assertAliveCells(int count) {
            assertEquals("Alive Cells: " + count, label.getText());
        }

        void clickCell(int column, int row) {
            board.dispatchEvent(new MouseEvent(board, MouseEvent.MOUSE_PRESSED,
                    0L, 0, cellCenter(column), cellCenter(row), 1, false, MouseEvent.BUTTON1));
        }

        int renderedCellColor(int column, int row) {
            BufferedImage image = new BufferedImage(board.getWidth(), board.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            try {
                board.paint(graphics);
                return image.getRGB(cellCenter(column), cellCenter(row));
            } finally {
                graphics.dispose();
            }
        }

        static JMenuItem menuItem(Object requester, String menu, String name) {
            JMenuItem item = MenuSite.getMyMenuItem(requester, menu, name);
            assertNotNull(item, menu + " > " + name + " menu item");
            return item;
        }

        private static int cellCenter(int coordinate) {
            return coordinate * CELL_PX + CELL_PX / 2;
        }
    }

    private static final class LoadDialog {
        private final Path file;
        private final Set<Window> existingWindows = Set.of(Window.getWindows());
        private final Timer timer = new Timer(25, event -> handleDialogs());
        private long deadline;
        private boolean fileApproved;
        private boolean timedOut;
        private boolean errorShown;

        private LoadDialog(Path file) {
            this.file = file;
        }

        private void run(JMenuItem load) {
            deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            timer.start();
            try {
                load.doClick(0);
            } finally {
                timer.stop();
                for (Window window : Window.getWindows()) {
                    if (window instanceof JDialog && !existingWindows.contains(window)) {
                        window.dispose();
                    }
                }
            }
            assertFalse(timedOut, "Timed out handling the Load dialog");
            assertTrue(fileApproved, "The temporary save should be approved by the Load chooser");
            assertFalse(errorShown, "Loading a valid save should not show an error dialog");
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
                    chooser.setSelectedFile(file.toFile());
                    chooser.approveSelection();
                    continue;
                }
                JOptionPane error = findComponent(dialog, JOptionPane.class);
                if (error != null) {
                    errorShown = true;
                    error.setValue(JOptionPane.OK_OPTION);
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

// Removing the panel ends its subscription for this frame. The guiTest task forks per class.
@Timeout(30)
class LifeStatisticsPanelRemovalGuiTest {

    @Test
    @DisplayName("Removing the Life statistics panel stops updates while board clicks still work")
    void stopsUpdatingStatisticsAfterPanelRemoval() throws Exception {
        LifeStatisticsIntegrationGuiTest.GameWindow game = LifeStatisticsIntegrationGuiTest.GameWindow.open();
        try {
            SwingUtilities.invokeAndWait(() -> {
                game.reset();
                game.clickCell(2, 3);
                game.assertAliveCells(1);
                int deadColor = game.renderedCellColor(10, 11);

                game.frame.getContentPane().remove(game.panel);
                assertFalse(game.panel.isDisplayable(), "The panel must no longer be attached to the frame");
                game.clickCell(10, 11);

                assertNotEquals(deadColor, game.renderedCellColor(10, 11), "The board still responds to clicks");
                game.assertAliveCells(1);
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    game.reset();
                } finally {
                    game.frame.dispose();
                }
            });
        }
    }
}
