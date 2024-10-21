package de.nvclas.flats.utils;

import de.nvclas.flats.Flats;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class I18n {

    private static final String BUNDLE_NAME = "i18n.lang";
    private static final String FALLBACK_LOCALE = "en_US";

    private static String currentLocale;
    private static ResourceBundle translations;
    private static ResourceBundle fallbackTranslations;

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

        try {
            Locale locale = Locale.forLanguageTag(localeCode.replace("_", "-")); // Korrekte Methode für Locale-Objekte
            translations = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            currentLocale = localeCode;

            if (FALLBACK_LOCALE.equals(localeCode)) {
                fallbackTranslations = translations;
            }

            getLogger().info("Loaded translation " + localeCode);
        } catch (MissingResourceException e) {
            getLogger().warning("Translation file not found for locale: " + localeCode);
        }
    }

    public static @NotNull String translate(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... args) {
        String translated = getTranslation(key, args);
        return translated != null ? translated : key;
    }

    private static @Nullable String getTranslation(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... args) {
        String translation = null;

        if (translations != null && translations.containsKey(key)) {
            translation = translations.getString(key);
        }

        if (translation == null && fallbackTranslations != null && fallbackTranslations.containsKey(key)) {
            translation = fallbackTranslations.getString(key);
        }

        return translation != null ? String.format(translation, args) : null;
    }

    private static @NotNull Logger getLogger() {
        return plugin.getLogger();
    }
}