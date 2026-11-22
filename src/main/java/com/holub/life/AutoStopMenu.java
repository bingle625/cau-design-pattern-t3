package com.holub.life;

import java.awt.Component;
import java.util.List;
import java.util.Objects;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.holub.ui.MenuSite;

/**
 * The "Auto Stop" menu. It lets the user pick a {@link StopCondition} and
 * connects an {@link AutoStopController} to the clock and the universe.
 *
 * <p>Neither the clock nor the universe is modified. The controller only
 * subscribes to notifications that both already publish, and stops the
 * clock through its existing {@link Clock#stop()} method.
 */
public final class AutoStopMenu {
    static final String MENU = "Auto Stop";
    static final String AFTER_GENERATIONS = "After N Generations...";
    static final String WHEN_EXTINCT = "When Extinct";
    static final String AT_POPULATION = "At Population...";
    static final String OFF = "Off";

    /** Interval used when "After N Generations..." starts the run (the Go menu's Medium). */
    static final int RUN_INTERVAL_MS = 70;

    private static final String CHECK = "✓ ";
    private static final List<String> CONDITION_ITEMS =
            List.of(AFTER_GENERATIONS, WHEN_EXTINCT, AT_POPULATION);

    private final Clock clock;
    private final Component dialogParent;
    private final AutoStopController controller;

    // Which menu item set the condition that is being watched. Touched on the EDT only.
    private StopCondition chosenCondition;
    private String chosenItem;

    /**
     * Adds the menu and starts observing. Call this after the universe has
     * been created: the controller must subscribe to the clock later than
     * the universe does, because the clock notifies its newest subscriber
     * first and the controller has to hear a tick before the universe
     * applies it.
     */
    public static AutoStopMenu install(Clock clock, Universe universe) {
        AutoStopMenu menu = new AutoStopMenu(clock, universe);
        clock.addClockListener(menu.controller);
        universe.addStatisticsListener(menu.controller);
        return menu;
    }

    private AutoStopMenu(Clock clock, Component dialogParent) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.dialogParent = Objects.requireNonNull(dialogParent, "dialogParent");
        this.controller = new AutoStopController(this::conditionMet);

        MenuSite.addLine(this, MENU, AFTER_GENERATIONS, event -> askAndRunGenerations());
        MenuSite.addLine(this, MENU, WHEN_EXTINCT, event -> stopWhenExtinct());
        MenuSite.addLine(this, MENU, AT_POPULATION, event -> askAndStopAtPopulation());
        MenuSite.addLine(this, MENU, "-", null);
        MenuSite.addLine(this, MENU, OFF, event -> turnOff());
        updateMenu();
    }

    /** Runs the given number of generations from now, then stops. */
    void runGenerations(int count) {
        choose(AFTER_GENERATIONS, new GenerationLimitCondition(count));
        clock.startTicking(RUN_INTERVAL_MS);
    }

    /** Stops the next time the board is empty after a generation. Does not start the clock. */
    void stopWhenExtinct() {
        choose(WHEN_EXTINCT, new ExtinctionCondition());
    }

    /** Stops once at least this many cells are alive after a generation. Does not start the clock. */
    void stopAtPopulation(int threshold) {
        choose(AT_POPULATION, new PopulationThresholdCondition(threshold));
    }

    /** Removes the condition. The clock keeps doing whatever it was doing. */
    void turnOff() {
        controller.disarm();
        updateMenu();
    }

    private void choose(String item, StopCondition condition) {
        chosenItem = item;
        chosenCondition = condition;
        controller.arm(condition);
        updateMenu();
    }

    /** Called by the controller, possibly on the timer thread. */
    private void conditionMet() {
        clock.stop();
        SwingUtilities.invokeLater(this::updateMenu);
    }

    private void askAndRunGenerations() {
        Integer count = askForPositiveNumber("Number of generations to run:", AFTER_GENERATIONS);
        if (count != null) {
            runGenerations(count);
        }
    }

    private void askAndStopAtPopulation() {
        Integer threshold = askForPositiveNumber("Stop when this many cells are alive:", AT_POPULATION);
        if (threshold != null) {
            stopAtPopulation(threshold);
        }
    }

    /** @return the number entered, or null if the user cancelled or the input was not a positive integer */
    private Integer askForPositiveNumber(String message, String title) {
        String input = JOptionPane.showInputDialog(dialogParent, message, title, JOptionPane.QUESTION_MESSAGE);
        if (input == null) {
            return null;
        }
        Integer number = parsePositiveNumber(input);
        if (number == null) {
            JOptionPane.showMessageDialog(dialogParent, "Enter a whole number of 1 or more.",
                    title, JOptionPane.ERROR_MESSAGE);
        }
        return number;
    }

    /** @return the positive integer in the text, or null if it is not one */
    static Integer parsePositiveNumber(String text) {
        try {
            int number = Integer.parseInt(text.trim());
            return number >= 1 ? number : null;
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    /** Puts a check mark on the item whose condition is still being watched. */
    private void updateMenu() {
        StopCondition armed = controller.armedCondition();
        String checkedItem = (armed != null && armed == chosenCondition) ? chosenItem : null;
        for (String item : CONDITION_ITEMS) {
            JMenuItem menuItem = MenuSite.getMyMenuItem(this, MENU, item);
            menuItem.setText(item.equals(checkedItem) ? CHECK + item : item);
        }
    }
}
