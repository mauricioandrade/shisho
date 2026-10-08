package dev.mauricioandrade.shisho.domain.exception;

/** The sources of the analyzed project could not be read. */
public final class SourceException extends ShishoException {

    public SourceException(String messageKey, Object... arguments) {
        super(messageKey, null, arguments);
    }

    public SourceException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause, arguments);
    }
}
