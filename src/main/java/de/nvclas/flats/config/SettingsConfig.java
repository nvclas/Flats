package de.nvclas.flats.config;

import de.nvclas.flats.Flats;

public class SettingsConfig extends Config {

    private static final String LANGUAGE_PATH = "language";
    private static final String MAX_FLAT_SIZE_PATH = "maxFlatSize";

    public SettingsConfig(Flats plugin, String fileName) {
        super(plugin, fileName);
    }

    public String getLanguage() {
        return getConfigFile().getString(LANGUAGE_PATH);
    }

    public int getMaxFlatSize() {
        return getConfigFile().getInt(MAX_FLAT_SIZE_PATH);
    }
}
