package de.nvclas.flats.util;

import lombok.experimental.UtilityClass;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.PropertyKey;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for internationalization (I18n) functionality. Provides methods for managing and
 * retrieving translations for a specified locale using resource bundles. This class supports
 * dynamic locale switching and fallback translations.
 * <p>
 * The class must be initialized with a plugin instance before its methods can be used.
 *
 * @see ResourceBundle
 * @see Locale
 */
@UtilityClass
public class I18n {

    private static final String BUNDLE_NAME = "i18n.lang";
    private static final String FALLBACK_LOCALE = "en_us";

    private static boolean initialized = false;
    private static String currentLocale;
    private static ResourceBundle translations;
    private static ResourceBundle fallbackTranslations;

    private static JavaPlugin plugin;

    /**
     * Initializes the I18n utility class with a specified {@link JavaPlugin}.
     * This method must be called before using any other method in the I18n class.
     * It also loads the translations for the default fallback locale.
     *
     * @param plugin The {@link JavaPlugin} instance to associate with the I18n class. Must not be null.
     */
    public static void initialize(JavaPlugin plugin) {
        I18n.plugin = plugin;
        initialized = true;
        loadTranslations(FALLBACK_LOCALE);
    }

    /**
     * Loads translations for the specified locale and updates the current translation set.
     * If the specified locale matches the fallback locale, it will also set the fallback translations.
     * Logs warnings if the translation file for the given locale is not found.
     * This method requires {@link I18n#initialize(JavaPlugin)} to be called first.
     *
     * @param localeCode The locale code to load translations for, represented as a {@code String}.
     *                   Examples include "en_us" or "de_de". Must not be null or empty.
     *                   If the locale matches the current locale, no action is taken.
     */
    public static void loadTranslations(String localeCode) {
        if (!initialized) {
            throw new IllegalStateException("I18n is not initialized! Call I18n.initialize() first");
        }
        if (localeCode == null || localeCode.isEmpty() || localeCode.equalsIgnoreCase(currentLocale)) {
            return;
        }

        try {
            Locale locale = Locale.forLanguageTag(localeCode.replace("_", "-")); // Korrekte Methode für Locale-Objekte
            translations = ResourceBundle.getBundle(BUNDLE_NAME, locale);
            currentLocale = localeCode;

            if (FALLBACK_LOCALE.equals(localeCode)) {
                fallbackTranslations = translations;
            }

            getLogger().log(Level.INFO, () -> "Loaded translation " + localeCode);
        } catch (MissingResourceException e) {
            getLogger().log(Level.WARNING, () -> "Translation file not found for locale: " + localeCode);
        }
    }

    /**
     * Translates a message key into the corresponding localized text using the current locale.
     * If the key cannot be found in the translation files, the key itself is returned as a fallback.
     *
     * @param key  The translation key to look up. This must match a key in the resource bundle. Must not be null.
     * @param args Optional arguments to format the translated message using {@link String#format}.
     * @return The localized text if the key is found, or the key itself if no translation is available.
     */
    public static @NotNull String translate(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... args) {
        String translated = getTranslation(key, args);
        return translated != null ? translated : key;
    }

    /**
     * Retrieves the translated text corresponding to the given key from the active translations or fallback translations.
     * If the key does not exist in either, returns {@code null}. Supports optional arguments
     * for formatting the translated text using {@link String#format}.
     *
     * @param key  The key for the desired translation. Must correspond to a valid key in the resource bundle.
     * @param args Optional arguments to format the translated text using {@link String#format}.
     * @return The formatted translation string if found, or {@code null} if no matching translation exists.
     */
    public static @Nullable String getTranslation(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... args) {
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