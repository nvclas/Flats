package de.nvclas.flats.config;

import de.nvclas.flats.Flats;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public abstract class Config {

    protected final File file;
    protected final Flats plugin;
    @Getter
    protected FileConfiguration configFile;

    protected Config(@NotNull Flats plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        saveDefaults();
        createConfig();
    }

    protected void createConfig() {
        try {
            if (!plugin.getDataFolder().mkdir() && !file.createNewFile()) {
                plugin.getLogger().config("File " + file.getName() + " already exists");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create file " + file.getName() + ": " + e.getMessage());
        }
        configFile = YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig() {
        try {
            configFile.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save file " + file.getName() + ": " + e.getMessage());
        }
        configFile = YamlConfiguration.loadConfiguration(file);
    }

    private void saveDefaults() {
        if (!file.exists()) {
            try {
                plugin.saveResource(file.getName(), false);
                plugin.getLogger().config("Saved dafault file of " + file.getName());
            } catch (IllegalArgumentException e) {
                plugin.getLogger().config("No default file found for " + file.getName());
            }
        }
    }
}
