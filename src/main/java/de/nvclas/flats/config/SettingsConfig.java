package de.nvclas.flats.config;

import de.nvclas.flats.Flats;

public class SettingsConfig extends Config {

    private static final String LANGUAGE_PATH = "language";
    private static final String MAX_FLAT_SIZE_PATH = "maxFlatSize";

    private static final String DEFAULT_LANGUAGE = "en_us";
    private static final int DEFAULT_MAX_FLAT_SIZE = 10000;

    public SettingsConfig(Flats plugin, String fileName) {
        super(plugin, fileName);
    }

    public String getLanguage() {
        return getConfigFile().getString(LANGUAGE_PATH);
    }

    public int getMaxFlatSize() {
        return getConfigFile().getInt(MAX_FLAT_SIZE_PATH);
    }

    @Override
    public void createConfig() {
        super.createConfig();
        configFile.addDefault(LANGUAGE_PATH, DEFAULT_LANGUAGE);
        configFile.addDefault(MAX_FLAT_SIZE_PATH, DEFAULT_MAX_FLAT_SIZE);
        configFile.options().copyDefaults(true);
        saveConfig();
    }

}
