package de.nvclas.flats.config;

import de.nvclas.flats.Flats;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public abstract class Config {

    protected FileConfiguration config;
    protected final File file;

    public Config(String fileName) {
        this.file = new File(Flats.getInstance().getDataFolder(), fileName);
        createConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    private void createConfig() {
        if (!file.exists()) {
            Flats.getInstance().getDataFolder().mkdir();
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public void saveConfig() {
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

}
