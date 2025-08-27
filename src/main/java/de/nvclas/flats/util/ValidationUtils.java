package de.nvclas.flats.util;

import de.nvclas.flats.Flats;
import lombok.experimental.UtilityClass;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Utility class for validating flat names and other user inputs.
 * Provides methods to ensure flat names meet quality standards and prevent issues.
 */
@UtilityClass
public class ValidationUtils {

    // Pattern for valid flat names: alphanumeric, underscore, dash, 3-32 characters
    private static final Pattern VALID_FLAT_NAME = Pattern.compile("^[a-zA-Z0-9_-]{3,32}$");
    
    // Reserved flat names that shouldn't be used
    private static final String[] RESERVED_NAMES = {
        "admin", "server", "console", "system", "plugin", "flats", "flat",
        "all", "everyone", "nobody", "null", "none", "default"
    };

    /**
     * Validates a flat name to ensure it meets quality standards.
     * 
     * @param player The player attempting to create/rename the flat
     * @param flatName The proposed flat name
     * @return true if the name is valid, false otherwise (with error message sent to player)
     */
    public boolean isValidFlatName(@NotNull Player player, @NotNull String flatName) {
        // Check length and character requirements
        if (!VALID_FLAT_NAME.matcher(flatName).matches()) {
            player.sendMessage(Flats.PREFIX + I18n.translate("validation.invalid_name"));
            player.sendMessage(Flats.PREFIX + I18n.translate("validation.name_requirements"));
            return false;
        }
        
        // Check reserved names
        for (String reserved : RESERVED_NAMES) {
            if (reserved.equalsIgnoreCase(flatName)) {
                player.sendMessage(Flats.PREFIX + I18n.translate("validation.reserved_name", flatName));
                return false;
            }
        }
        
        return true;
    }

    /**
     * Provides suggestions for fixing an invalid flat name.
     * 
     * @param invalidName The invalid name to provide suggestions for
     * @return A suggested valid name
     */
    public @NotNull String suggestValidName(@NotNull String invalidName) {
        // Remove invalid characters and ensure proper length
        String suggested = invalidName
            .replaceAll("[^a-zA-Z0-9_-]", "_")
            .toLowerCase();
        
        // Ensure minimum length
        if (suggested.length() < 3) {
            suggested = suggested + "_flat";
        }
        
        // Ensure maximum length
        if (suggested.length() > 32) {
            suggested = suggested.substring(0, 32);
        }
        
        // Avoid reserved names by adding suffix
        for (String reserved : RESERVED_NAMES) {
            if (reserved.equals(suggested)) {
                suggested = suggested + "_1";
                break;
            }
        }
        
        return suggested;
    }
}