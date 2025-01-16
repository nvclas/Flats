package de.nvclas.flats.config;

public class SettingsConfig extends Config {

    private static final String LANGUAGE_PATH = "language";
    private static final String MAX_FLAT_SIZE_PATH = "maxFlatSize";

    private static final String ENABLE_AUTO_GAMEMODE_PATH = "enableAutoGamemode";
    private static final String INSIDE_GAMEMODE_PATH = "insideFlatGamemode";
    private static final String OUTSIDE_GAMEMODE_PATH = "outsideFlatGamemode";

    public SettingsConfig(String fileName) {
        super(fileName);
    }

    public String getLanguage() {
        return getConfigFile().getString(LANGUAGE_PATH);
    }

    public int getMaxFlatSize() {
        return getConfigFile().getInt(MAX_FLAT_SIZE_PATH);
    }

    public boolean isAutoGamemodeEnabled() {
        return getConfigFile().getBoolean(ENABLE_AUTO_GAMEMODE_PATH);
    }
    
    public String getInsideGamemode() {
        return getConfigFile().getString(INSIDE_GAMEMODE_PATH);
    }

    public String getOutsideGamemode() {
        return getConfigFile().getString(OUTSIDE_GAMEMODE_PATH);
    }
}
