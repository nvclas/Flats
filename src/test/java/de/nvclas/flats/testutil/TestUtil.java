package de.nvclas.flats.testutil;

import lombok.experimental.UtilityClass;
import org.junit.jupiter.api.Assertions;

@UtilityClass
public class TestUtil {

    public static void assertEqualMessage(String expected, String actual) {
        expected = expected.replaceAll("§.", "");
        actual = actual.replaceAll("§.", "");
        Assertions.assertEquals(expected, actual);
    }

}
