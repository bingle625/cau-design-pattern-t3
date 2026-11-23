package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.MenuSelectionManager;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.holub.ui.MenuSite;

@Execution(ExecutionMode.SAME_THREAD)
class UniverseViewMenuGuiTest {
    private static final String NORMAL = "Normal";
    private static final String AGE = "Show Cell Age";
    private static final String NEWBORN = "Highlight Newborn";
    private static final String SURVIVORS = "Highlight Survivors";
    private static final int CELL_SIZE = 8;

    private static JFrame frame;
    private static Universe universe;
    private static Clock clock;

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
            MenuSite.getMyMenuItem(universe, "Grid", "Clear").doClick();
            viewItem(NORMAL).doClick();
        });
    }

    @AfterAll
    static void disposeFrame() throws Exception {
        SwingUtilities.invokeAndWait(() -> frame.dispose());
    }

    @Test
    @DisplayName("The View menu starts on Normal with every effect off")
    void startsOnNormalView() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("✓ " + NORMAL, viewItem(NORMAL).getText());
            assertEquals(AGE, viewItem(AGE).getText());
            assertEquals(NEWBORN, viewItem(NEWBORN).getText());
            assertEquals(SURVIVORS, viewItem(SURVIVORS).getText());
        });
    }

    @Test
    @DisplayName("Effects are toggled independently and can be on together")
    void effectsToggleIndependently() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            viewItem(AGE).doClick();
            viewItem(NEWBORN).doClick();

            assertEquals(NORMAL, viewItem(NORMAL).getText());
            assertEquals("✓ " + AGE, viewItem(AGE).getText());
            assertEquals("✓ " + NEWBORN, viewItem(NEWBORN).getText());
            assertEquals(SURVIVORS, viewItem(SURVIVORS).getText());

            viewItem(AGE).doClick();

            assertEquals(AGE, viewItem(AGE).getText());
            assertEquals("✓ " + NEWBORN, viewItem(NEWBORN).getText());
        });
    }

    @Test
    @DisplayName("Normal turns every effect off at once")
    void normalClearsEveryEffect() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            viewItem(AGE).doClick();
            viewItem(SURVIVORS).doClick();

            viewItem(NORMAL).doClick();

            assertEquals("✓ " + NORMAL, viewItem(NORMAL).getText());
            assertEquals(AGE, viewItem(AGE).getText());
            assertEquals(SURVIVORS, viewItem(SURVIVORS).getText());
        });
    }

    @Test
    @DisplayName("Highlight Newborn frames a newly placed cell and Normal removes the frame")
    void newbornEffectIsPaintedOnTheBoard() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(3, 4);
            assertEquals(BasicRenderer.LIVE_COLOR, framePixel(3, 4));

            viewItem(NEWBORN).doClick();
            assertEquals(NewbornDecorator.FRAME_COLOR, framePixel(3, 4));
            assertEquals(BasicRenderer.LIVE_COLOR, centerPixel(3, 4));

            viewItem(NORMAL).doClick();
            assertEquals(BasicRenderer.LIVE_COLOR, framePixel(3, 4));
        });
    }

    @Test
    @DisplayName("A still life changes color and gains the survivor frame as generations pass")
    void stillLifeAgesWhileTheBoardKeepsChanging() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            placeBlock(2, 2);
            placeBlinker(20, 20);       // keeps the board changing so ticks advance
            viewItem(AGE).doClick();
            viewItem(SURVIVORS).doClick();
        });
        Color young = onEdt(() -> centerPixel(2, 2));

        for (int tick = 0; tick < SurvivorDecorator.SURVIVOR_AGE; ++tick) {
            SwingUtilities.invokeAndWait(clock::tick);
        }

        SwingUtilities.invokeAndWait(() -> {
            Color old = centerPixel(2, 2);
            assertEquals(BasicRenderer.LIVE_COLOR, young);
            assertNotEquals(young, old);
            assertTrue(old.getBlue() > young.getBlue(), "the old block moved toward the oldest color");
            assertEquals(SurvivorDecorator.FRAME_COLOR, framePixel(2, 2));
        });
    }

    @Test
    @DisplayName("Undo restarts the age of the restored cells")
    void undoRestartsAge() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            placeBlock(2, 2);
            placeBlinker(20, 20);
            viewItem(AGE).doClick();
        });
        for (int tick = 0; tick < 3; ++tick) {
            SwingUtilities.invokeAndWait(clock::tick);
        }
        Color aged = onEdt(() -> centerPixel(2, 2));

        assertTrue(universe.undo());

        SwingUtilities.invokeAndWait(() -> {
            assertNotEquals(BasicRenderer.LIVE_COLOR, aged);
            assertEquals(BasicRenderer.LIVE_COLOR, centerPixel(2, 2));
        });
    }

    private static JMenuItem viewItem(String name) {
        return MenuSite.getMyMenuItem(universe, "View", name);
    }

    private static void placeBlock(int column, int row) {
        clickCell(column, row);
        clickCell(column + 1, row);
        clickCell(column, row + 1);
        clickCell(column + 1, row + 1);
    }

    private static void placeBlinker(int column, int row) {
        clickCell(column, row);
        clickCell(column + 1, row);
        clickCell(column + 2, row);
    }

    private static void clickCell(int column, int row) {
        int x = column * CELL_SIZE + CELL_SIZE / 2;
        int y = row * CELL_SIZE + CELL_SIZE / 2;
        MouseEvent press = new MouseEvent(universe, MouseEvent.MOUSE_PRESSED,
                System.currentTimeMillis(), 0, x, y, 1, false, MouseEvent.BUTTON1);
        universe.dispatchEvent(press);
    }

    private static Color centerPixel(int column, int row) {
        return pixel(column * CELL_SIZE + CELL_SIZE / 2, row * CELL_SIZE + CELL_SIZE / 2);
    }

    /** A pixel on the frame the highlight effects draw inside a cell. */
    private static Color framePixel(int column, int row) {
        return pixel(column * CELL_SIZE + 1, row * CELL_SIZE + CELL_SIZE / 2);
    }

    private static Color pixel(int x, int y) {
        BufferedImage image = new BufferedImage(universe.getWidth(), universe.getHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        try {
            universe.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return new Color(image.getRGB(x, y), true);
    }

    private static Color onEdt(java.util.concurrent.Callable<Color> read) throws Exception {
        Color[] result = new Color[1];
        Exception[] failure = new Exception[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                result[0] = read.call();
            } catch (Exception e) {
                failure[0] = e;
            }
        });
        if (failure[0] != null) {
            throw failure[0];
        }
        return result[0];
    }
}
