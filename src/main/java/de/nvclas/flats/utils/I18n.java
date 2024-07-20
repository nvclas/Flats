package de.nvclas.flats.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.nvclas.flats.Flats;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class I18n {

    private static final Gson gson = new Gson();
    private static final String BASE_PATH = "i18n/";
    private static final String FILE_EXTENSION = ".json";
    private static final String FALLBACK_LOCALE = "en_us";

    private static String currentLocale;
    private static Map<String, String> translations = new HashMap<>();
    private static Map<String, String> fallbackTranslations = new HashMap<>();

    private static Flats plugin;

    private I18n() {
        throw new IllegalStateException("Utility class");
    }

    public static void initialize(Flats pluginInstance) {
        plugin = pluginInstance;
        loadTranslations(FALLBACK_LOCALE);
    }

    public static void loadTranslations(String localeCode) {
        if (localeCode == null || localeCode.isEmpty() || localeCode.equals(currentLocale)) {
            return;
        }

        String filePath = BASE_PATH + localeCode + FILE_EXTENSION;
        InputStream resource = plugin.getResource(filePath);

        if (resource == null) {
            getLogger().warning(() -> "Translation file not found: " + filePath);
            return;
        }

        try (InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> jsonMap = gson.fromJson(reader, type);
            Map<String, String> flattenedMap = flattenMap(jsonMap);

            if (FALLBACK_LOCALE.equals(localeCode)) {
                fallbackTranslations = flattenedMap;
            } else {
                translations = flattenedMap;
            }
            currentLocale = localeCode;
            getLogger().info(() -> "Loaded translation " + localeCode);
        } catch (Exception e) {
            getLogger().warning("Failed to load translations for " + localeCode + ": " + e.getMessage());
        }
    }

    public static String translate(String key, Object... args) {
        String translated = getTranslation(key, args);
        return translated != null ? translated : key;
    }

    private static String getTranslation(String key, Object... args) {
        String translation = translations.getOrDefault(key, fallbackTranslations.get(key));
        return translation != null ? String.format(translation, args) : null;
    }

    private static Map<String, String> flattenMap(Map<String, Object> map) {
        Map<String, String> flatMap = new HashMap<>();
        flattenMapHelper("", map, flatMap);
        return flatMap;
    }

    @SuppressWarnings("unchecked")
    private static void flattenMapHelper(String prefix, Map<String, Object> map, Map<String, String> flatMap) {
        map.forEach((key, value) -> {
            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map) {
                flattenMapHelper(fullKey, (Map<String, Object>) value, flatMap);
            } else {
                flatMap.put(fullKey, value.toString());
            }
        });
    }

    private static Logger getLogger() {
        return plugin.getLogger();
    }
}