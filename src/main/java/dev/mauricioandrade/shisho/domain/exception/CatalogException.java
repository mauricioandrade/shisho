package dev.mauricioandrade.shisho.domain.exception;

/** The catalog could not be loaded. */
public final class CatalogException extends ShishoException {

    public CatalogException(String messageKey, Object... arguments) {
        super(messageKey, null, arguments);
    }

    public CatalogException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause, arguments);
    }
}
