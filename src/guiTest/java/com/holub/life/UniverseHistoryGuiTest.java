package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

@Execution(ExecutionMode.SAME_THREAD)
class UniverseHistoryGuiTest {
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
            if (listener != null) {
                universe.removeStatisticsListener(listener);
            }
            clearBoard();
        });
    }

    @AfterAll
    static void disposeFrame() throws Exception {
        SwingUtilities.invokeAndWait(() -> frame.dispose());
    }

    @Test
    @DisplayName("Each manual tick records one completed generation")
    void recordsManualTicks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clickCell(3, 4);
            clickCell(4, 4);
            clickCell(5, 4);
            assertEquals(0, universe.generationHistorySize());

            clock.tick();
            assertEquals(1, universe.generationHistorySize());
            clock.tick();
            assertEquals(2, universe.generationHistorySize());
        });
    }

    @Test
    @DisplayName("An unchanged board still records each normally completed tick")
    void recordsUnchangedTicks() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.tick();
            clock.tick();

            assertEquals(2, universe.generationHistorySize());
        });
    }

    @Test
    @DisplayName("A cell click clears the previous generation timeline")
    void clearsHistoryAfterClick() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.tick();
            assertEquals(1, universe.generationHistorySize());

            clickCell(3, 4);

            assertEquals(0, universe.generationHistorySize());
        });
    }

    @Test
    @DisplayName("Clear discards generation history even when the board is empty")
    void clearsHistoryAfterClear() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            clock.tick();
            assertEquals(1, universe.generationHistorySize());

            clearBoard();

            assertEquals(0, universe.generationHistorySize());
        });
    }

    @Test
    @DisplayName("The running app retains only the most recent ten generation commands")
    void boundsApplicationHistory() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (int generation = 0; generation < 11; generation++) {
                clock.tick();
            }

            assertEquals(10, universe.generationHistorySize());
        });
    }

    @Test
    @DisplayName("Automatic ticks keep their background thread and share the manual tick history")
    void recordsAutomaticAndManualTicks() throws Exception {
        BlockingQueue<Boolean> callbacksOnEdt = new LinkedBlockingQueue<>();
        SwingUtilities.invokeAndWait(() -> {
            listener = statistics -> callbacksOnEdt.add(SwingUtilities.isEventDispatchThread());
            universe.addStatisticsListener(listener);
            clock.startTicking(60_000);
        });

        assertEquals(Boolean.FALSE, callbacksOnEdt.poll(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            assertEquals(1, universe.generationHistorySize());
            clock.tick();
            assertEquals(2, universe.generationHistorySize());
        });
        assertEquals(Boolean.TRUE, callbacksOnEdt.poll(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("A click waits for an automatic generation to finish before clearing its history")
    void serializesClickWithAutomaticGeneration() throws Exception {
        BlockingQueue<String> events = new LinkedBlockingQueue<>();
        CountDownLatch finishTick = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        SwingUtilities.invokeAndWait(() -> {
            listener = statistics -> {
                if (first.compareAndSet(true, false)) {
                    events.add("automatic started");
                    try {
                        assertTrue(finishTick.await(5, TimeUnit.SECONDS), "The test must release the tick");
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(exception);
                    }
                    events.add("automatic finished");
                }
            };
            universe.addStatisticsListener(listener);
            clock.startTicking(60_000);
        });
        try {
            assertEquals("automatic started", events.poll(5, TimeUnit.SECONDS));
            SwingUtilities.invokeLater(() -> {
                clickCell(3, 4);
                events.add("click finished");
            });

            assertNull(events.poll(100, TimeUnit.MILLISECONDS), "A click must wait for the model work");
        } finally {
            finishTick.countDown();
        }

        assertEquals("automatic finished", events.poll(5, TimeUnit.SECONDS));
        assertEquals("click finished", events.poll(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            clock.stop();
            assertEquals(0, universe.generationHistorySize());
        });
    }

    private static void clearBoard() {
        JMenuItem clear = MenuSite.getMyMenuItem(universe, "Grid", "Clear");
        assertNotNull(clear);
        clear.doClick(0);
    }

    private static void clickCell(int column, int row) {
        universe.dispatchEvent(new MouseEvent(universe, MouseEvent.MOUSE_PRESSED,
                0L, 0, column * 8 + 4, row * 8 + 4, 1, false, MouseEvent.BUTTON1));
    }
}
