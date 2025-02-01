package de.nvclas.flats.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Paths {
    public static final String LANGUAGE = "language";
    public static final String MAX_FLAT_SIZE = "maxFlatSize";
    public static final String ENABLE_AUTO_GAMEMODE = "enableAutoGamemode";
    public static final String INSIDE_GAMEMODE = "insideFlatGamemode";
    public static final String OUTSIDE_GAMEMODE = "outsideFlatGamemode";
    
    public static String getOwnerPath(String flatName) {
        return "flats." + flatName + ".owner";
    } 
    
    public static String getAreasPath(String flatName) {
        return "flats." + flatName + ".areas";
    }
    
}