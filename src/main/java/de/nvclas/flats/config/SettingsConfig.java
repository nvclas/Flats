package de.nvclas.flats.config;

import de.nvclas.flats.Flats;

public class SettingsConfig extends Config {
    
    public SettingsConfig(Flats plugin, String fileName) {
        super(plugin, fileName);
    }
    
    public String getLanguage() {
       return getConfigFile().getString("language");
    } 
    
}
