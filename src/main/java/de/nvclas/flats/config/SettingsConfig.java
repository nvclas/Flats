package de.nvclas.flats.config;

public class SettingsConfig extends Config {

    public SettingsConfig(String fileName) {
        super(fileName);
    }

    private <T> T getConfigValue(String path, Class<T> type) {
        if (type == String.class) {
            return type.cast(getConfigFile().getString(path));
        } else if (type == Integer.class) {
            return type.cast(getConfigFile().getInt(path));
        } else if (type == Boolean.class) {
            return type.cast(getConfigFile().getBoolean(path));
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    public String getLanguage() {
        return getConfigValue(Paths.LANGUAGE, String.class);
    }

    public int getMaxFlatSize() {
        return getConfigValue(Paths.MAX_FLAT_SIZE, Integer.class);
    }

    public boolean isAutoGamemodeEnabled() {
        return getConfigValue(Paths.ENABLE_AUTO_GAMEMODE, Boolean.class);
    }

    public String getInsideGamemode() {
        return getConfigValue(Paths.INSIDE_GAMEMODE, String.class);
    }

    public String getOutsideGamemode() {
        return getConfigValue(Paths.OUTSIDE_GAMEMODE, String.class);
    }
}
