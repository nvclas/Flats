package de.nvclas.flats.testutil;

import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.Assertions;

/**
 * Utility class providing helper methods for testing Minecraft-specific functionality.
 */
@UtilityClass
public class TestUtil {

    /**
     * Asserts that two messages are equal after removing Minecraft color codes.
     * Minecraft uses the section symbol (§) followed by a character to represent colors and formatting.
     * This method strips these codes before comparing the messages.
     *
     * @param expected The expected message text
     * @param actual   The actual message text received
     */
    public static void assertEqualMessage(String expected, String actual) {
        expected = expected.replaceAll("§.", "");
        actual = actual.replaceAll("§.", "");
        Assertions.assertEquals(expected, actual);
    }

}
