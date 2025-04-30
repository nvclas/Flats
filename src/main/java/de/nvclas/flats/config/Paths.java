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

    public static final String FLATS = "flats";

    private final String rootSection = "flats.";

    public static String getOwnerPath(String flatName) {
        return rootSection + flatName + ".owner";
    }

    public static String getAreasPath(String flatName) {
        return rootSection + flatName + ".areas";
    }

    public static String getTrustedPath(String flatName) {
        return rootSection + flatName + ".trusted";
    }

}