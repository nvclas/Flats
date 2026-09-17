package de.nvclas.flats.core.config;

import de.nvclas.flats.core.BaseFlats;
import org.bukkit.GameMode;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The {@code SettingsConfig} class is a specific implementation of the {@link Config} class
 * that handles configuration settings related to flat management and player preferences.
 * <p>
 * Default values are defined for various configuration settings, which are returned in
 * case no specific value is set in the configuration file. This class provides methods
 * to access these settings.
 */
public class SettingsConfig extends Config implements ConfigAdapter {

    private static final String PATH_LANGUAGE = "language";
    private static final String PATH_MAX_FLAT_SIZE = "maxFlatSize";
    private static final String PATH_ENABLE_AUTO_GAMEMODE = "enableAutoGamemode";
    private static final String PATH_INSIDE_GAMEMODE = "insideFlatGamemode";
    private static final String PATH_OUTSIDE_GAMEMODE = "outsideFlatGamemode";
    private static final String PATH_MAX_CLAIMABLE_FLATS = "maxClaimableFlats";
    private static final String PATH_USE_ADVANCED_PERMISSIONS = "useAdvancedPermissions";

    private static final String DEFAULT_LANGUAGE = "en_US";
    private static final int DEFAULT_MAX_FLAT_SIZE = 10000;
    private static final int DEFAULT_MAX_CLAIMABLE_FLATS = 3;
    private static final boolean DEFAULT_USE_ADVANCED_PERMISSIONS = false;
    private static final boolean DEFAULT_ENABLE_AUTO_GAMEMODE = false;
    private static final GameMode DEFAULT_INSIDE_GAMEMODE = GameMode.CREATIVE;
    private static final GameMode DEFAULT_OUTSIDE_GAMEMODE = GameMode.ADVENTURE;

    /**
     * Constructs a new {@code SettingsConfig} instance with the specified file name and plugin reference.
     * <p>
     * This constructor initializes the settings configuration by loading the specified configuration file.
     * If the file doesn't exist, it will be created with default values.
     *
     * @param fileName The name of the configuration file. Must not be null.
     * @param plugin   The plugin instance that owns this configuration. Must not be null.
     */
    public SettingsConfig(String fileName, BaseFlats plugin) {
        super(fileName, plugin);
    }

    /**
     * Retrieves the configured language value from the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return the current language setting as a non-null {@code String}.
     */
    @Override
    public @NotNull String getLanguage() {
        return getConfigFile().getString(PATH_LANGUAGE, DEFAULT_LANGUAGE);
    }


    /**
     * Retrieves the maximum allowed size for a flat from the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return the maximum flat size as an {@code int}.
     */
    @Override
    public int getMaxFlatSize() {
        return getConfigFile().getInt(PATH_MAX_FLAT_SIZE, DEFAULT_MAX_FLAT_SIZE);
    }

    /**
     * Retrieves the maximum number of flats a player is allowed to claim from the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return the maximum claimable flats as an {@code int}.
     */
    @Override
    public int getMaxClaimableFlats() {
        return getConfigFile().getInt(PATH_MAX_CLAIMABLE_FLATS, DEFAULT_MAX_CLAIMABLE_FLATS);
    }

    /**
     * Retrieves the advanced permissions setting from the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return {@code true} if advanced permissions are enabled; {@code false} otherwise.
     */
    @Override
    public boolean isAdvancedPermissionsEnabled() {
        return getConfigFile().getBoolean(PATH_USE_ADVANCED_PERMISSIONS, DEFAULT_USE_ADVANCED_PERMISSIONS);
    }

    /**
     * Checks whether the automatic gamemode switch is enabled in the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return {@code true} if auto gamemode is enabled; {@code false} otherwise.
     */
    @Override
    public boolean isAutoGamemodeEnabled() {
        return getConfigFile().getBoolean(PATH_ENABLE_AUTO_GAMEMODE, DEFAULT_ENABLE_AUTO_GAMEMODE);
    }

    /**
     * Retrieves the configured gamemode setting for when a player is inside a flat.
     * <p>
     * If no specific value is set in the configuration, a default value is returned.
     *
     * @return the inside gamemode setting as a non-null {@code String}.
     */
    @Override
    public @NotNull GameMode getInsideGamemode() {
        return getGameMode(PATH_INSIDE_GAMEMODE, DEFAULT_INSIDE_GAMEMODE);
    }

    /**
     * Retrieves the configured gamemode to be applied outside the defined flat areas.
     * <p>
     * If no specific gamemode is set in the configuration, a default value is returned.
     *
     * @return the outside gamemode setting as a non-null {@code String}.
     */
    @Override
    public @NotNull GameMode getOutsideGamemode() {
        return getGameMode(PATH_OUTSIDE_GAMEMODE, DEFAULT_OUTSIDE_GAMEMODE);
    }

    private @NotNull GameMode getGameMode(@NotNull String path, @NotNull GameMode defaultGameMode) {
        String configuredGameMode = getConfigFile().getString(path);

        if (configuredGameMode == null) {
            return defaultGameMode;
        }

        try {
            return GameMode.valueOf(configuredGameMode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return defaultGameMode;
        }
    }
}
