package dev.mauricioandrade.shisho.infrastructure.i18n;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * The languages the Shishō speaks, chosen with {@code SHISHO_IDIOMA}: the messages' locale and the
 * catalog folder holding the translated text. Portuguese is the catalog's source language, so it
 * has no translation folder. See docs/adr/0007-languages.md.
 */
public enum Language {

    PORTUGUESE("pt", Messages.DEFAULT_LOCALE, Optional.empty()),
    ENGLISH("en", Locale.ENGLISH, Optional.of("en"));

    public static final String VARIABLE = "SHISHO_IDIOMA";

    private final String code;
    private final Locale locale;
    private final Optional<String> catalogTranslation;

    Language(String code, Locale locale, Optional<String> catalogTranslation) {
        this.code = code;
        this.locale = locale;
        this.catalogTranslation = catalogTranslation;
    }

    /** {@code pt} or {@code en}, in any case; unset or blank is Portuguese. */
    public static Optional<Language> of(String code) {
        if (code == null || code.isBlank()) {
            return Optional.of(PORTUGUESE);
        }
        return Arrays.stream(values()).filter(language -> language.code.equalsIgnoreCase(code.strip())).findFirst();
    }

    public Locale locale() {
        return locale;
    }

    public Optional<String> catalogTranslation() {
        return catalogTranslation;
    }
}
