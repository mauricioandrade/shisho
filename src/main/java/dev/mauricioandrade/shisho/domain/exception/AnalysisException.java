package dev.mauricioandrade.shisho.domain.exception;

/** The project cannot be analyzed as it is (e.g. its Java version is not supported). */
public final class AnalysisException extends ShishoException {

    public AnalysisException(String messageKey, Object... arguments) {
        super(messageKey, null, arguments);
    }

    public AnalysisException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause, arguments);
    }
}
