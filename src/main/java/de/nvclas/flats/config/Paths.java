package de.nvclas.flats.config;

import lombok.experimental.UtilityClass;

/**
 * Utility class that provides configuration path constants for flat-related settings and utility methods
 * to dynamically generate specific configuration paths.
 * <p>
 * The {@code Paths} class contains predefined strings representing various configuration keys that are
 * frequently used in flat management within a configuration file. It also includes methods to generate
 * specific configuration paths based on flat names, such as paths for flat owners and flat areas.
 */
@UtilityClass
public class Paths {
    public static final String LANGUAGE = "language";
    public static final String AUTO_SAVE_INTERVAL = "autoSaveInterval";
    public static final String MAX_FLAT_SIZE = "maxFlatSize";
    public static final String ENABLE_AUTO_GAMEMODE = "enableAutoGamemode";
    public static final String INSIDE_GAMEMODE = "insideFlatGamemode";
    public static final String OUTSIDE_GAMEMODE = "outsideFlatGamemode";
    public static final String MAX_CLAIMABLE_FLATS = "maxClaimableFlats";
    public static final String USE_ADVANCED_PERMISSIONS = "useAdvancedPermissions";

    public static final String FLATS = "flats";

    private static final String ROOT_SECTION = "flats.";

    /**
     * Generates the configuration path for a flat's owner.
     * <p>
     * This method constructs the full path string used to access or store
     * the owner information for a specific flat in the configuration.
     *
     * @param flatName The name of the flat. Must not be null.
     * @return A string representing the configuration path for the flat's owner.
     */
    public static String getOwnerPath(String flatName) {
        return ROOT_SECTION + flatName + ".owner";
    }

    /**
     * Generates the configuration path for a flat's areas.
     * <p>
     * This method constructs the full path string used to access or store
     * the area information for a specific flat in the configuration.
     *
     * @param flatName The name of the flat. Must not be null.
     * @return A string representing the configuration path for the flat's areas.
     */
    public static String getAreasPath(String flatName) {
        return ROOT_SECTION + flatName + ".areas";
    }

    /**
     * Generates the configuration path for a flat's trusted players.
     * <p>
     * This method constructs the full path string used to access or store
     * the list of trusted players for a specific flat in the configuration.
     *
     * @param flatName The name of the flat. Must not be null.
     * @return A string representing the configuration path for the flat's trusted players.
     */
    public static String getTrustedPath(String flatName) {
        return ROOT_SECTION + flatName + ".trusted";
    }

}
