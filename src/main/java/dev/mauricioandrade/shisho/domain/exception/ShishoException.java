package dev.mauricioandrade.shisho.domain.exception;

import java.util.Arrays;
import java.util.List;

/**
 * A problem the user must fix. Carries an i18n message key and its arguments so the terminal
 * adapter can explain it in the user's language.
 */
public abstract sealed class ShishoException extends RuntimeException
        permits AnalysisException, BuildFileException, CatalogException, ConfigException, LlmException, SourceException {

    private final String messageKey;
    private final List<Object> arguments;

    ShishoException(String messageKey, Throwable cause, Object... arguments) {
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
