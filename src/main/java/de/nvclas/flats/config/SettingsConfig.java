package de.nvclas.flats.config;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * The {@code SettingsConfig} class is responsible for managing the retrieval of specific
 * configuration settings related to flat and gameplay customization.
 * <p>
 * This class extends {@link Config}, inheriting capabilities to interface with
 * configuration files and simplify the management of plugin-specific settings.
 * {@code SettingsConfig} provides convenient methods for retrieving various
 * configuration values such as language settings, maximum flat sizes, and game modes.
 */
public class SettingsConfig extends Config {

    public SettingsConfig(String fileName, JavaPlugin plugin) {
        super(fileName, plugin);
    }

    /**
     * Retrieves the configured language setting.
     * <p>
     * The language setting determines the localization that the application should use,
     * which may affect in-game messages and other textual content.
     *
     * @return the language code as a {@link String}, typically representing
     * a locale such as "en_US" or "de_DE".
     */
    public String getLanguage() {
        return getConfigValue(Paths.LANGUAGE, String.class);
    }

    /**
     * Retrieves the maximum allowed flat size from the configuration.
     * <p>
     * This value is defined in the configuration file using the path
     * specified by {@link Paths#MAX_FLAT_SIZE}. It represents the
     * maximum size that a flat can occupy.
     *
     * @return the maximum flat size as an integer, or a default
     * value if the configuration entry is not found.
     */
    public int getMaxFlatSize() {
        return getConfigValue(Paths.MAX_FLAT_SIZE, Integer.class);
    }

    /**
     * Checks whether the automatic gamemode feature is enabled in the configuration.
     * <p>
     * This method retrieves the value associated with the {@link Paths#ENABLE_AUTO_GAMEMODE} configuration key
     * and returns it as a boolean. The feature might control automatic switching or enabling of specific game modes
     * based on certain conditions.
     *
     * @return {@code true} if the automatic gamemode feature is enabled, {@code false} otherwise.
     */
    public boolean isAutoGamemodeEnabled() {
        return getConfigValue(Paths.ENABLE_AUTO_GAMEMODE, Boolean.class);
    }

    /**
     * Retrieves the configured game mode to be applied when inside a flat area.
     * <p>
     * This value is fetched from the configuration file using the path specified
     * in {@link Paths#INSIDE_GAMEMODE}.
     *
     * @return the name of the game mode as a string, or {@code null} if the
     * configuration value is not set or invalid.
     */
    public String getInsideGamemode() {
        return getConfigValue(Paths.INSIDE_GAMEMODE, String.class);
    }

    /**
     * Retrieves the configured game mode for players outside designated flat areas.
     * <p>
     * This method fetches the value associated with the {@code Paths.OUTSIDE_GAMEMODE} key from the configuration
     * file. The returned game mode is expected to be defined as a {@code String}, typically representing
     * one of the standard Minecraft game modes (e.g., "SURVIVAL", "CREATIVE").
     *
     * @return the game mode for players outside flat areas as a {@code String}, or {@code null} if not specified.
     */
    public String getOutsideGamemode() {
        return getConfigValue(Paths.OUTSIDE_GAMEMODE, String.class);
    }

    /**
     * Retrieves the interval, in milliseconds, at which data is automatically saved.
     * The value is fetched from the configuration file.
     *
     * @return the auto-save interval in milliseconds as a {@code long}.
     */
    public long getAutoSaveInterval() {
        return getConfigValue(Paths.AUTO_SAVE_INTERVAL, Long.class);
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
}
