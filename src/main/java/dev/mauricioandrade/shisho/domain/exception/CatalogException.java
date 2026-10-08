package dev.mauricioandrade.shisho.domain.exception;

import java.util.Arrays;
import java.util.List;

/**
 * The catalog could not be loaded. Carries an i18n message key and its arguments so the
 * terminal adapter can explain the problem to the user in their language.
 */
public final class CatalogException extends RuntimeException {

    private final String messageKey;
    private final List<Object> arguments;

    public CatalogException(String messageKey, Object... arguments) {
        this(messageKey, null, arguments);
    }

    public CatalogException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey + " " + Arrays.toString(arguments), cause);
        this.messageKey = messageKey;
        this.arguments = List.of(arguments);
    }

    public String messageKey() {
        return messageKey;
    }

    public List<Object> arguments() {
        return arguments;
    }
}
