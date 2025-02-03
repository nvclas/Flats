package de.nvclas.flats.utils;

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
 * Utility class to provide internationalization (i18n) support for translations using resource bundles.
 * The class enables translation of strings based on locale-specific keys, allowing dynamic language and region handling.
 * It requires initialization with a plugin instance before its methods can be used.
 * <p>
 * Internally, the class uses a fallback locale to ensure graceful handling of missing translations.
 * Methods support formatted string replacements and rely on {@link ResourceBundle} for managing language resources.
 */
@UtilityClass
public class I18n {

    private static final String BUNDLE_NAME = "i18n.lang";
    private static final String FALLBACK_LOCALE = "en_US";

    private static boolean initialized = false;
    private static String currentLocale;
    private static ResourceBundle translations;
    private static ResourceBundle fallbackTranslations;

    private static JavaPlugin plugin;

    /**
     * Initializes the I18n system by setting the plugin instance and performing the initial
     * translation loading for the default fallback locale. This method must be called before
     * using any of the translation-related functionality in the {@link I18n} class.
     *
     * @param plugin The {@link JavaPlugin} instance representing the plugin. This is used
     *               to access resources and manage the plugin's operation.
     */
    public static void initialize(JavaPlugin plugin) {
        I18n.plugin = plugin;
        initialized = true;
        loadTranslations(FALLBACK_LOCALE);
    }

    /**
     * Loads translations for the specified locale code. This method updates the current translation
     * resources using the provided locale code, replacing the existing translations.
     * If the locale code matches the fallback locale, the fallback translations are also updated.
     * If the locale code is invalid, empty, or matches the current locale, the method does nothing.
     * Logs a message to indicate the loaded locale or a warning if a translation file is not found.
     *
     * @param localeCode The locale code (e.g., "en_US") specifying the language and region for which
     *                   translations will be loaded. It must follow the format used by {@link Locale}.
     *                   If the locale code is not present in the resource bundle, a warning will be logged.
     */
    public static void loadTranslations(String localeCode) {
        if (!initialized) {
            throw new IllegalStateException("I18n is not initialized! Call I18n.initialize() first");
        }
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

            getLogger().log(Level.INFO, () -> "Loaded translation " + localeCode);
        } catch (MissingResourceException e) {
            getLogger().log(Level.WARNING, () -> "Translation file not found for locale: " + localeCode);
        }
    }

    /**
     * Translates the given key into the current locale's language, using the resource bundle and optional arguments.
     * If a translation is not found for the key, the key itself is returned as a fallback.
     *
     * @param key  The key to be translated, referencing a string in the resource bundle.
     * @param args Optional arguments to format the translated string, if placeholders are present in it.
     * @return A non-null string containing the translated text. If no translation is found,
     * the original key is returned as the fallback.
     */
    public static @NotNull String translate(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... args) {
        String translated = getTranslation(key, args);
        return translated != null ? translated : key;
    }

    /**
     * Retrieves the translation for the specified key from the current or fallback resource bundles.
     * If the key is not found, null is returned. If a translation is found and contains placeholders,
     * they are formatted using the provided arguments.
     *
     * @param key  The key to be translated, referencing a string in the resource bundle. Must be present in the resource bundle.
     * @param args Optional arguments to format the translated string, if placeholders are present within it.
     * @return A translated string formatted with the provided arguments, or {@code null} if no translation is found.
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