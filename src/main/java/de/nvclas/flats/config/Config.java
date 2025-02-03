package de.nvclas.flats.config;

import de.nvclas.flats.Flats;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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

    protected final File file;

    @Getter
    @NotNull
    protected FileConfiguration configFile;

    @SuppressWarnings("java:S2637") // initializeConfig already sets the value
    protected Config(String fileName) {
        this.file = new File(Flats.getInstance().getDataFolder(), fileName);
        saveDefaultConfig();
        initializeConfig();
    }

    private static void createParentDirectory() {
        File dataFolder = Flats.getInstance().getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdir()) {
            Flats.getInstance().getLogger().log(Level.CONFIG, () -> "Failed to create plugin data folder.");
        }
    }

    protected void initializeConfig() {
        try {
            createParentDirectory();
            if (!file.createNewFile()) {
                Flats.getInstance()
                        .getLogger()
                        .log(Level.CONFIG, () -> String.format(CONFIG_FILE_EXISTS, file.getName()));
            }
        } catch (IOException e) {
            Flats.getInstance()
                    .getLogger()
                    .log(Level.SEVERE, () -> String.format(CONFIG_CREATION_FAILURE, file.getName(), e.getMessage()));
        }
        configFile = YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig() {
        try {
            configFile.save(file);
        } catch (IOException e) {
            Flats.getInstance()
                    .getLogger()
                    .log(Level.SEVERE, () -> String.format(CONFIG_SAVE_FAILURE, file.getName(), e.getMessage()));
        }
        configFile = YamlConfiguration.loadConfiguration(file);
    }

    private void saveDefaultConfig() {
        if (!file.exists()) {
            try {
                Flats.getInstance().saveResource(file.getName(), false);
                Flats.getInstance()
                        .getLogger()
                        .log(Level.CONFIG, () -> String.format(CONFIG_SAVED_DEFAULT, file.getName()));
            } catch (IllegalArgumentException e) {
                Flats.getInstance().getLogger().log(Level.CONFIG, () -> String.format(CONFIG_DEFAULT_NOT_FOUND, file.getName()));
            }
        }
    }
}