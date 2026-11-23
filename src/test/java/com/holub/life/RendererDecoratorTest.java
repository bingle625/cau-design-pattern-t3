package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RendererDecoratorTest {
    private static final Rectangle CELL = new Rectangle(0, 0, 8, 8);
    private static final Color LIVE = BasicRenderer.LIVE_COLOR;
    private static final Color DEAD = BasicRenderer.DEAD_COLOR;

    private final BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    private final Graphics graphics = image.getGraphics();

    @Test
    @DisplayName("A decorator needs a renderer to wrap")
    void decoratorRejectsMissingRenderer() {
        assertThrows(NullPointerException.class, () -> new AgeDecorator(null));
    }

    @Test
    @DisplayName("The wrapped renderer draws before the effect is painted over it")
    void innerRendererDrawsFirst() {
        List<String> events = new ArrayList<>();
        CellRenderer inner = (g, here, alive, age) -> events.add("inner");
        CellRenderer decorated = new RendererDecorator(inner) {
            @Override
            protected void decorate(Graphics g, Rectangle here, boolean alive, int age) {
                events.add("effect");
            }
        };

        decorated.draw(graphics, CELL, true, 1);

        assertEquals(List.of("inner", "effect"), events);
    }

    @Test
    @DisplayName("The caller's graphics color is restored after every effect")
    void graphicsColorIsRestored() {
        graphics.setColor(Color.MAGENTA);

        allEffects().draw(graphics, CELL, true, SurvivorDecorator.SURVIVOR_AGE);

        assertEquals(Color.MAGENTA, graphics.getColor());
    }

    @Test
    @DisplayName("Age shading leaves a newly appeared cell at the plain live color")
    void ageLeavesNewCellUntouched() {
        new AgeDecorator(new BasicRenderer()).draw(graphics, CELL, true, 1);

        assertEquals(LIVE, center());
    }

    @Test
    @DisplayName("Age shading moves a cell further from the live color with every generation")
    void ageShiftsOlderCellsTowardOldestColor() {
        CellRenderer renderer = new AgeDecorator(new BasicRenderer());

        renderer.draw(graphics, CELL, true, 3);
        Color young = center();
        renderer.draw(graphics, CELL, true, 6);
        Color old = center();

        assertTrue(young.getBlue() > LIVE.getBlue(), "an aged cell has left the live color");
        assertTrue(old.getBlue() > young.getBlue(), "an older cell is closer to the oldest color");
        assertTrue(old.getRed() < young.getRed());
    }

    @Test
    @DisplayName("Successive ages differ enough to be told apart at a glance")
    void neighboringAgesAreClearlyDifferent() {
        CellRenderer renderer = new AgeDecorator(new BasicRenderer());

        for (int age = 1; age < AgeDecorator.OLDEST_AGE; ++age) {
            renderer.draw(graphics, CELL, true, age);
            Color younger = center();
            renderer.draw(graphics, CELL, true, age + 1);
            Color older = center();

            assertTrue(older.getBlue() - younger.getBlue() >= 30,
                    "age " + age + " and " + (age + 1) + " look alike");
        }
    }

    @Test
    @DisplayName("The oldest cells are drawn in the oldest color")
    void oldestCellReachesOldestColor() {
        new AgeDecorator(new BasicRenderer()).draw(graphics, CELL, true, AgeDecorator.OLDEST_AGE);

        assertEquals(AgeDecorator.OLDEST_COLOR, center());
    }

    @Test
    @DisplayName("Age shading stops changing past the oldest age")
    void ageShadeIsCapped() {
        CellRenderer renderer = new AgeDecorator(new BasicRenderer());

        renderer.draw(graphics, CELL, true, AgeDecorator.OLDEST_AGE);
        Color oldest = center();
        renderer.draw(graphics, CELL, true, AgeDecorator.OLDEST_AGE * 100);

        assertEquals(oldest, center());
    }

    @Test
    @DisplayName("The newborn frame marks only cells in their first generation")
    void newbornFramesFirstGenerationOnly() {
        CellRenderer renderer = new NewbornDecorator(new BasicRenderer());

        renderer.draw(graphics, CELL, true, 1);
        assertEquals(NewbornDecorator.FRAME_COLOR, frame());
        assertEquals(LIVE, center(), "the frame does not cover the cell");

        renderer.draw(graphics, CELL, true, 2);
        assertEquals(LIVE, frame());
    }

    @Test
    @DisplayName("The survivor frame marks only cells that reached the survivor age")
    void survivorFramesOldCellsOnly() {
        CellRenderer renderer = new SurvivorDecorator(new BasicRenderer());

        renderer.draw(graphics, CELL, true, SurvivorDecorator.SURVIVOR_AGE - 1);
        assertEquals(LIVE, frame());

        renderer.draw(graphics, CELL, true, SurvivorDecorator.SURVIVOR_AGE);
        assertEquals(SurvivorDecorator.FRAME_COLOR, frame());
        assertEquals(LIVE, center(), "the frame does not cover the cell");
    }

    @Test
    @DisplayName("No effect touches a dead cell")
    void deadCellIsNeverDecorated() {
        allEffects().draw(graphics, CELL, false, 0);

        assertEquals(DEAD, center());
        assertEquals(DEAD, frame());
    }

    @Test
    @DisplayName("Stacked effects are all visible on the same cell")
    void effectsCombine() {
        allEffects().draw(graphics, CELL, true, SurvivorDecorator.SURVIVOR_AGE);

        assertEquals(SurvivorDecorator.FRAME_COLOR, frame());
        assertNotEquals(LIVE, center(), "the age shade shows inside the survivor frame");
    }

    @Test
    @DisplayName("Frames stay on top whichever order the effects are stacked in")
    void framesSurviveAnyStackingOrder() {
        CellRenderer ageOutside = new AgeDecorator(new NewbornDecorator(new BasicRenderer()));

        ageOutside.draw(graphics, CELL, true, 1);

        assertEquals(NewbornDecorator.FRAME_COLOR, frame());
    }

    private static CellRenderer allEffects() {
        return new SurvivorDecorator(new NewbornDecorator(new AgeDecorator(new BasicRenderer())));
    }

    private Color center() {
        return new Color(image.getRGB(4, 4), true);
    }

    /** A pixel on the frame drawn along the inner edge of the cell. */
    private Color frame() {
        return new Color(image.getRGB(1, 4), true);
    }
}
