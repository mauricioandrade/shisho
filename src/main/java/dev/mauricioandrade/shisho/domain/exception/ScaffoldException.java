package dev.mauricioandrade.shisho.domain.exception;

/** The CRUD cannot be generated as asked: an invalid name or type, or no architecture to follow. */
public final class ScaffoldException extends ShishoException {

    public ScaffoldException(String messageKey, Object... arguments) {
        super(messageKey, null, arguments);
    }
}
