package com.holub.life;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AutoStopMenuInputTest {

    @Test
    @DisplayName("A positive whole number is accepted, with or without surrounding spaces")
    void acceptsPositiveWholeNumbers() {
        assertEquals(1, AutoStopMenu.parsePositiveNumber("1"));
        assertEquals(100, AutoStopMenu.parsePositiveNumber("100"));
        assertEquals(7, AutoStopMenu.parsePositiveNumber("  7 "));
    }

    @Test
    @DisplayName("Zero and negative numbers are rejected")
    void rejectsZeroAndNegativeNumbers() {
        assertNull(AutoStopMenu.parsePositiveNumber("0"));
        assertNull(AutoStopMenu.parsePositiveNumber("-3"));
    }

    @Test
    @DisplayName("Text that is not a whole number is rejected")
    void rejectsTextThatIsNotAWholeNumber() {
        assertNull(AutoStopMenu.parsePositiveNumber(""));
        assertNull(AutoStopMenu.parsePositiveNumber("abc"));
        assertNull(AutoStopMenu.parsePositiveNumber("1.5"));
        assertNull(AutoStopMenu.parsePositiveNumber("99999999999"));
    }
}
