package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.holub.ui.MenuSite;

class UniverseInitializationGuiTest {

    @Test
    @DisplayName("Universe initializes on the EDT with a display and an established menu site")
    void initializesUniverseWithMenuSite() throws Exception {
        assertFalse(GraphicsEnvironment.isHeadless());

        SwingUtilities.invokeAndWait(() -> {
            JFrame frame = new JFrame();
            try {
                MenuSite.establish(frame);
                Universe universe = Universe.instance();
                assertNotNull(universe);

                frame.add(universe);
                frame.pack();
                assertTrue(frame.isDisplayable());
            } finally {
                frame.dispose();
            }
        });
    }
}
