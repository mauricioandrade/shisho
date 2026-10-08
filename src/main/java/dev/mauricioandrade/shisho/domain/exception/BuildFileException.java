package dev.mauricioandrade.shisho.domain.exception;

/** The build file of the analyzed project could not be read. */
public final class BuildFileException extends ShishoException {

    public BuildFileException(String messageKey, Object... arguments) {
        super(messageKey, null, arguments);
    }

    public BuildFileException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause, arguments);
    }
}
