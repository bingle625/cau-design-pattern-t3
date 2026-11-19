package com.holub.life;

import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class StatisticsPanel extends JPanel implements StatisticsListener {

    private static final String ALIVE_CELLS_PREFIX = "Alive Cells: ";

    private final JLabel aliveCellsLabel;
    private final AtomicLong latestNotification = new AtomicLong();

    /** Creates the panel on the Swing event dispatch thread. */
    public StatisticsPanel() {
        aliveCellsLabel = new JLabel(ALIVE_CELLS_PREFIX + "0");
        add(aliveCellsLabel);
    }

    @Override
    public void statisticsChanged(GameStatistics statistics) {
        long notification = latestNotification.incrementAndGet();
        Runnable updateLabel = () -> {
            // A newer callback may have arrived while this update was queued.
            if (notification == latestNotification.get()) {
                aliveCellsLabel.setText(ALIVE_CELLS_PREFIX + statistics.getAliveCells());
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            updateLabel.run();
        } else {
            SwingUtilities.invokeLater(updateLabel);
        }
    }
}
