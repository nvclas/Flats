package de.nvclas.flats.config;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Represents a configuration wrapper specifically designed for handling settings
 * related to a flat-based management system.
 * <p>
 * This class extends the {@link Config} class to provide specific methods for
 * retrieving configuration values related to:<p>
 * - General settings (e.g., language and auto-save intervals)<p>
 * - Flat-specific settings (e.g., maximum flat size, auto gamemode toggles)<p>
 * - Game mode configurations for player behavior based on position (inside vs. outside a flat)
 * <p>
 * Internally, the class uses the {@link #getConfigValue(String, Class)} helper method
 * to fetch configuration values and cast them to the required data type.
 * This ensures data integrity when interacting with a YAML configuration file.
 */
public class SettingsConfig extends Config {

    public SettingsConfig(String fileName, JavaPlugin plugin) {
        super(fileName, plugin);
    }

    private <T> T getConfigValue(String path, Class<T> type) {
        if (type == String.class) {
            return type.cast(getConfigFile().getString(path));
        } else if (type == Integer.class) {
            return type.cast(getConfigFile().getInt(path));
        } else if (type == Boolean.class) {
            return type.cast(getConfigFile().getBoolean(path));
        } else if (type == Long.class) {
            return type.cast(getConfigFile().getLong(path));
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    /**
     * Retrieves the language setting from the configuration file.
     * The language setting is defined by the key {@link Paths#LANGUAGE}.
     * This method accesses the value corresponding to the {@link Paths#LANGUAGE}
     * path in the configuration file and returns it as a {@code String}.
     *
     * @return the configured language value as a {@code String}.
     */
    public String getLanguage() {
        return getConfigValue(Paths.LANGUAGE, String.class);
    }

    /**
     * Retrieves the maximum flat size configuration value.
     * <p>
     * The value is fetched from the configuration file using the
     * {@link Paths#MAX_FLAT_SIZE} path and is expected to be an integer.
     * This method uses the {@link #getConfigValue(String, Class)} helper method
     * to perform the retrieval and casting of the value.
     *
     * @return the maximum flat size as an integer, as specified in the configuration.
     */
    public int getMaxFlatSize() {
        return getConfigValue(Paths.MAX_FLAT_SIZE, Integer.class);
    }

    /**
     * Checks whether the auto gamemode feature is enabled in the configuration.
     * <p>
     * The auto gamemode feature automatically switches the player's gamemode
     * when they enter or leave a flat based on the configuration settings.
     * <p>
     * This method retrieves the value associated with {@link Paths#ENABLE_AUTO_GAMEMODE}
     * from the configuration file and returns it as a boolean.
     *
     * @return {@code true} if the auto gamemode feature is enabled in the configuration;
     * {@code false} otherwise.
     */
    public boolean isAutoGamemodeEnabled() {
        return getConfigValue(Paths.ENABLE_AUTO_GAMEMODE, Boolean.class);
    }

    /**
     * Retrieves the configured game mode for being inside an owned flat.
     * The value is fetched from the configuration using the key defined in
     * {@link Paths#INSIDE_GAMEMODE}.
     *
     * @return the game mode as a {@code String} for when inside a flat area.
     * This value is obtained from the configuration file.
     */
    public String getInsideGamemode() {
        return getConfigValue(Paths.INSIDE_GAMEMODE, String.class);
    }

    /**
     * Retrieves the game mode to be set for a player when outside of a flat area.
     * <p>
     * This value is fetched from the configuration file using the path specified in
     * {@link Paths#OUTSIDE_GAMEMODE}.
     *
     * @return The outside game mode as a {@link String}. The returned string corresponds
     * to the configured game mode for players when outside of flat areas.
     */
    public String getOutsideGamemode() {
        return getConfigValue(Paths.OUTSIDE_GAMEMODE, String.class);
    }

    /**
     * Retrieves the auto-save interval configuration value.
     * <p>
     * This method fetches the value of the {@link Paths#AUTO_SAVE_INTERVAL} configuration
     * entry as a {@code long}. It uses the {@code getConfigValue} method to retrieve the value
     * from the configuration file.
     *
     * @return the auto-save interval in seconds as a {@code long}
     */
    public long getAutoSaveInterval() {
        return getConfigValue(Paths.AUTO_SAVE_INTERVAL, Long.class);
    }
}
