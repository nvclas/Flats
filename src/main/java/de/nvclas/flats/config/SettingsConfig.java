package de.nvclas.flats.config;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code SettingsConfig} class is a specific implementation of the {@link Config} class
 * that handles configuration settings related to flat management and player preferences.
 * <p>
 * Default values are defined for various configuration settings, which are returned in
 * case no specific value is set in the configuration file. This class provides methods
 * to access these settings.
 */
public class SettingsConfig extends Config {

    private static final String DEFAULT_LANGUAGE = "en_US";
    private static final long DEFAULT_AUTO_SAVE_INTERVAL = 600;
    private static final int DEFAULT_MAX_FLAT_SIZE = 10000;
    private static final int DEFAULT_MAX_CLAIMABLE_FLATS = 1;
    private static final boolean DEFAULT_ENABLE_AUTO_GAMEMODE = false;
    private static final String DEFAULT_INSIDE_GAMEMODE = "creative";
    private static final String DEFAULT_OUTSIDE_GAMEMODE = "adventure";

    public SettingsConfig(String fileName, JavaPlugin plugin) {
        super(fileName, plugin);
    }

    /**
     * Retrieves the configured language value from the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return the current language setting as a non-null {@code String}.
     */
    @NotNull
    public String getLanguage() {
        return getConfigValue(Paths.LANGUAGE, String.class, DEFAULT_LANGUAGE);
    }

    /**
     * Retrieves the auto-save interval setting from the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return the auto-save interval in milliseconds as a {@code long}.
     */
    public long getAutoSaveInterval() {
        return getConfigValue(Paths.AUTO_SAVE_INTERVAL, Long.class, DEFAULT_AUTO_SAVE_INTERVAL);
    }

    /**
     * Retrieves the maximum allowed size for a flat from the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return the maximum flat size as an {@code int}.
     */
    public int getMaxFlatSize() {
        return getConfigValue(Paths.MAX_FLAT_SIZE, Integer.class, DEFAULT_MAX_FLAT_SIZE);
    }

    /**
     * Retrieves the maximum number of flats a player is allowed to claim from the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return the maximum claimable flats as an {@code int}.
     */
    public int getMaxClaimableFlats() {
        return getConfigValue(Paths.MAX_CLAIMABLE_FLATS, Integer.class, DEFAULT_MAX_CLAIMABLE_FLATS);
    }

    /**
     * Checks whether the automatic gamemode switch is enabled in the configuration.
     * <p>
     * If the value is not explicitly set in the configuration, a default value is returned.
     *
     * @return {@code true} if auto gamemode is enabled; {@code false} otherwise.
     */
    public boolean isAutoGamemodeEnabled() {
        return getConfigValue(Paths.ENABLE_AUTO_GAMEMODE, Boolean.class, DEFAULT_ENABLE_AUTO_GAMEMODE);
    }

    /**
     * Retrieves the configured gamemode setting for when a player is inside a flat.
     * <p>
     * If no specific value is set in the configuration, a default value is returned.
     *
     * @return the inside gamemode setting as a non-null {@code String}.
     */
    @NotNull
    public String getInsideGamemode() {
        return getConfigValue(Paths.INSIDE_GAMEMODE, String.class, DEFAULT_INSIDE_GAMEMODE);
    }

    /**
     * Retrieves the configured gamemode to be applied outside the defined flat areas.
     * <p>
     * If no specific gamemode is set in the configuration, a default value is returned.
     *
     * @return the outside gamemode setting as a non-null {@code String}.
     */
    @NotNull
    public String getOutsideGamemode() {
        return getConfigValue(Paths.OUTSIDE_GAMEMODE, String.class, DEFAULT_OUTSIDE_GAMEMODE);
    }

    private <T> T getConfigValue(String path, Class<T> type, T defaultValue) {
        if (type == String.class) {
            return type.cast(getConfigFile().getString(path, (String) defaultValue));
        } else if (type == Integer.class) {
            return type.cast(getConfigFile().getInt(path, (Integer) defaultValue));
        } else if (type == Boolean.class) {
            return type.cast(getConfigFile().getBoolean(path, (Boolean) defaultValue));
        } else if (type == Long.class) {
            return type.cast(getConfigFile().getLong(path, (Long) defaultValue));
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }
}
