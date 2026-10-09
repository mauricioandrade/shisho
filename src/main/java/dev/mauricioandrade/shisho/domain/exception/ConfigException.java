package dev.mauricioandrade.shisho.domain.exception;

/** The analyzed project's configuration is unreadable or invalid. */
public final class ConfigException extends ShishoException {

    public ConfigException(String messageKey, Object... arguments) {
        super(messageKey, null, arguments);
    }

    public ConfigException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause, arguments);
    }
}
