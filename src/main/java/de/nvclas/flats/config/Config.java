package de.nvclas.flats.config;

import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public abstract class Config {

    private static final String CONFIG_CREATION_SUCCESS = "Created file %s";
    private static final String CONFIG_CREATION_FAILURE = "Failed to create file %s: %s";
    private static final String CONFIG_SAVED_DEFAULT = "Saved default file of %s";
    private static final String CONFIG_DEFAULT_NOT_FOUND = "No default file found for %s";

    protected final JavaPlugin plugin;
    protected final File file;

    @NotNull
    @Getter
    protected FileConfiguration configFile;

    @SuppressWarnings("java:S2637") // initializeConfig already sets the value
    protected Config(String fileName, JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        saveDefaultConfig();
        initializeConfig();
    }

    /**
     * Initializes the configuration file for the plugin. This method ensures that the
     * configuration file exists, and if not, attempts to create it. Once the file
     * is validated or created, it is loaded into a {@link YamlConfiguration} object.
     * <p>
     * If the configuration file already exists, a log message is generated with a
     * {@link Level#CONFIG} severity. If file creation fails, an error message is
     * logged with a {@link Level#SEVERE} severity, including details about the
     * failure.
     * <p>
     * Callers can use this method to guarantee the configuration file is properly
     * prepared and ready for use.
     */
    protected void initializeConfig() {
        try {
            createParentDirectory();
            if (file.createNewFile()) {
                plugin.getLogger().log(Level.CONFIG, () -> String.format(CONFIG_CREATION_SUCCESS, file.getName()));
            }
        } catch (IOException e) {
            plugin.getLogger()
                    .log(Level.SEVERE, () -> String.format(CONFIG_CREATION_FAILURE, file.getName(), e.getMessage()));
        }
        configFile = YamlConfiguration.loadConfiguration(file);
    }

    private void createParentDirectory() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            throw new IllegalStateException("Failed to create plugin data folder: " + dataFolder.getAbsolutePath());
        }
    }

    private void saveDefaultConfig() {
        if (!file.exists()) {
            try {
                plugin.saveResource(file.getName(), false);
                plugin.getLogger().log(Level.CONFIG, () -> String.format(CONFIG_SAVED_DEFAULT, file.getName()));
            } catch (IllegalArgumentException _) {
                plugin.getLogger().log(Level.CONFIG, () -> String.format(CONFIG_DEFAULT_NOT_FOUND, file.getName()));
            }
        }
    }
}
