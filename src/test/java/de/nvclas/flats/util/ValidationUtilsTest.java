package de.nvclas.flats.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.MockBukkitExtension;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.ServerMock;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link ValidationUtils} class.
 */
@ExtendWith(MockBukkitExtension.class)
@DisplayName("Validation Utils Tests")
class ValidationUtilsTest {

    @ParameterizedTest(name = "Valid name: \"{0}\"")
    @ValueSource(strings = {"test", "my_flat", "apartment-1", "test123", "a1b2c3", "valid_name_here"})
    @DisplayName("Valid flat names should pass validation")
    void validFlatNames(String validName) {
        ServerMock server = MockBukkit.getMock();
        PlayerMock player = server.addPlayer();
        
        assertTrue(ValidationUtils.isValidFlatName(player, validName),
                  "Name '" + validName + "' should be valid");
    }

    @ParameterizedTest(name = "Invalid name: \"{0}\"")
    @ValueSource(strings = {"ab", "a", "toolongflatnamethatshouldnotbeacceptedbecauseitover32chars", "test@name", "flat name", "test.dot", "name!"})
    @DisplayName("Invalid flat names should fail validation")
    void invalidFlatNames(String invalidName) {
        ServerMock server = MockBukkit.getMock();
        PlayerMock player = server.addPlayer();
        
        assertFalse(ValidationUtils.isValidFlatName(player, invalidName),
                   "Name '" + invalidName + "' should be invalid");
    }

    @ParameterizedTest(name = "Reserved name: \"{0}\"")
    @ValueSource(strings = {"admin", "server", "console", "system", "plugin", "flats", "flat", "all", "everyone", "null"})
    @DisplayName("Reserved names should be rejected")
    void reservedNames(String reservedName) {
        ServerMock server = MockBukkit.getMock();
        PlayerMock player = server.addPlayer();
        
        assertFalse(ValidationUtils.isValidFlatName(player, reservedName),
                   "Reserved name '" + reservedName + "' should be rejected");
        assertFalse(ValidationUtils.isValidFlatName(player, reservedName.toUpperCase()),
                   "Reserved name '" + reservedName.toUpperCase() + "' should be rejected (case insensitive)");
    }

    @ParameterizedTest(name = "Suggestion for \"{0}\" should be \"{1}\"")
    @CsvSource({
        "ab, ab_flat",
        "test@name, test_name",
        "my flat, my_flat",
        "toolongflatnamethatshouldnotbeacceptedbecauseitover32chars, toolongflatnamethatshouldnotbeacce",
        "admin, admin_1",
        "server, server_1"
    })
    @DisplayName("Name suggestions should be valid and helpful")
    void nameSuggestions(String invalidName, String expectedSuggestion) {
        String actualSuggestion = ValidationUtils.suggestValidName(invalidName);
        assertEquals(expectedSuggestion, actualSuggestion,
                    "Suggestion for '" + invalidName + "' should be '" + expectedSuggestion + "'");
        
        // Verify the suggestion itself is valid
        ServerMock server = MockBukkit.getMock();
        PlayerMock player = server.addPlayer();
        assertTrue(ValidationUtils.isValidFlatName(player, actualSuggestion),
                  "Suggested name '" + actualSuggestion + "' should be valid");
    }

    @Test
    @DisplayName("Suggestion method handles edge cases")
    void suggestionEdgeCases() {
        // Test empty string
        String suggestion = ValidationUtils.suggestValidName("");
        assertFalse(suggestion.isEmpty(), "Suggestion for empty string should not be empty");
        
        // Test null handling (though method expects NotNull)
        assertDoesNotThrow(() -> ValidationUtils.suggestValidName("test"),
                          "Method should handle normal input without throwing");
    }
}