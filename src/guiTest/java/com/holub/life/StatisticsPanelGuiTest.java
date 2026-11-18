package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StatisticsPanelGuiTest {

    private static final int TIMEOUT_SECONDS = 5;

    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private StatisticsPanel panel;
    private JLabel aliveCellsLabel;

    @BeforeEach
    void createPanelOnEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel = new StatisticsPanel();
            assertEquals(1, panel.getComponentCount(), "The panel should contain one statistics label");
            aliveCellsLabel = assertInstanceOf(JLabel.class, panel.getComponent(0));
        });
    }

    @AfterEach
    void stopBackgroundExecutor() throws InterruptedException {
        background.shutdownNow();
        assertTrue(background.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS),
                "The background test thread must stop");
    }

    @Test
    @DisplayName("The panel initially displays zero live cells")
    void displaysZeroInitially() throws Exception {
        SwingUtilities.invokeAndWait(() -> assertEquals("Alive Cells: 0", aliveCellsLabel.getText()));
    }

    @Test
    @DisplayName("An EDT notification updates the label before the callback returns")
    void updatesImmediatelyOnEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            panel.statisticsChanged(new GameStatistics(3));

            assertEquals("Alive Cells: 3", aliveCellsLabel.getText());
        });
    }

    @Test
    @DisplayName("A background notification changes the label on the EDT")
    void updatesLabelOnEdtAfterBackgroundNotification() throws Exception {
        ConcurrentLinkedQueue<Boolean> changesOnEdt = new ConcurrentLinkedQueue<>();
        SwingUtilities.invokeAndWait(() -> aliveCellsLabel.addPropertyChangeListener("text",
                event -> changesOnEdt.add(SwingUtilities.isEventDispatchThread())));

        background.submit(() -> panel.statisticsChanged(new GameStatistics(5)))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        SwingUtilities.invokeAndWait(() -> {
            assertEquals("Alive Cells: 5", aliveCellsLabel.getText());
            assertEquals(List.of(true), List.copyOf(changesOnEdt), "Label text changes must occur on the EDT");
        });
    }

    @Test
    @DisplayName("The latest of three consecutive background notifications is displayed")
    void displaysLatestConsecutiveNotification() throws Exception {
        background.submit(() -> {
            panel.statisticsChanged(new GameStatistics(1));
            panel.statisticsChanged(new GameStatistics(2));
            panel.statisticsChanged(new GameStatistics(3));
        }).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        SwingUtilities.invokeAndWait(() -> assertEquals("Alive Cells: 3", aliveCellsLabel.getText()));
    }

    @Test
    @DisplayName("An older queued update cannot overwrite a newer EDT notification")
    void ignoresOlderUpdateQueuedBeforeNewerEdtNotification() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            // The current EDT task prevents the older queued update from running yet.
            CompletableFuture.runAsync(() -> panel.statisticsChanged(new GameStatistics(1)), background)
                    .orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .join();
            assertEquals("Alive Cells: 0", aliveCellsLabel.getText(), "The older update is still queued");

            panel.statisticsChanged(new GameStatistics(3));
            assertEquals("Alive Cells: 3", aliveCellsLabel.getText(), "The newer EDT update is immediate");
        });

        SwingUtilities.invokeAndWait(() -> assertEquals("Alive Cells: 3", aliveCellsLabel.getText(),
                "Processing the older queued update must leave the newer value visible"));
    }
}
