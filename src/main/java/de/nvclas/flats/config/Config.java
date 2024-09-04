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
    @Getter
    protected FileConfiguration configFile;
    protected final Flats plugin;

    protected Config(@NotNull Flats plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        createConfig();
    }

    protected void createConfig() {
        try {
            if (!plugin.getDataFolder().mkdir() && !file.createNewFile()) {
                plugin.getLogger().info(() -> "File " + file.getName() + " already exists");
            }
        } catch (IOException e) {
            plugin.getLogger().severe(() -> "Failed to create file " + file.getName() + ": " + e.getMessage());
        }
        configFile = YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig() {
        try {
            configFile.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe(() -> "Failed to save file " + file.getName() + ": " + e.getMessage());
        }
        configFile = YamlConfiguration.loadConfiguration(file);
    }

}
