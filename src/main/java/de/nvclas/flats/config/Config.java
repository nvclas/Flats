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

    private static final String CONFIG_FILE_EXISTS = "File %s already exists";
    private static final String CONFIG_CREATION_FAILURE = "Failed to create file %s: %s";
    private static final String CONFIG_SAVED_DEFAULT = "Saved default file of %s";
    private static final String CONFIG_DEFAULT_NOT_FOUND = "No default file found for %s";
    private static final String CONFIG_SAVE_FAILURE = "Failed to save file %s: %s";

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

    private void createParentDirectory() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            plugin.getLogger().log(Level.CONFIG, () -> "Failed to create plugin data folder.");
        }
    }

    protected void initializeConfig() {
        try {
            createParentDirectory();
            if (!file.createNewFile()) {
                plugin.getLogger().log(Level.CONFIG, () -> String.format(CONFIG_FILE_EXISTS, file.getName()));
            }
        } catch (IOException e) {
            plugin.getLogger()
                    .log(Level.SEVERE, () -> String.format(CONFIG_CREATION_FAILURE, file.getName(), e.getMessage()));
        }
        configFile = YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig() {
        try {
            configFile.save(file);
        } catch (IOException e) {
            plugin.getLogger()
                    .log(Level.SEVERE, () -> String.format(CONFIG_SAVE_FAILURE, file.getName(), e.getMessage()));
        }
        configFile = YamlConfiguration.loadConfiguration(file);
    }

    private void saveDefaultConfig() {
        if (!file.exists()) {
            try {
                plugin.saveResource(file.getName(), false);
                plugin.getLogger().log(Level.CONFIG, () -> String.format(CONFIG_SAVED_DEFAULT, file.getName()));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().log(Level.CONFIG, () -> String.format(CONFIG_DEFAULT_NOT_FOUND, file.getName()));
            }
        }
    }
}