package dev.mauricioandrade.shisho.infrastructure.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/** User-facing texts from {@code messages*.properties}, formatted with {@link MessageFormat}. */
public final class Messages {

    /** The Shishō speaks Brazilian Portuguese; {@code messages.properties} (English) is the fallback. */
    public static final Locale DEFAULT_LOCALE = Locale.of("pt", "BR");

    private static final String BUNDLE = "messages";

    private final ResourceBundle bundle;

    public Messages(Locale locale) {
        this.bundle = ResourceBundle.getBundle(BUNDLE, locale, ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
    }

    public String text(String key, Object... arguments) {
        return new MessageFormat(bundle.getString(key), bundle.getLocale()).format(arguments);
    }

    public ResourceBundle bundle() {
        return bundle;
    }
}
