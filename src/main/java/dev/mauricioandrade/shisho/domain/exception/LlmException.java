package dev.mauricioandrade.shisho.domain.exception;

/** The language model could not answer: the server is down, or the model is not installed. */
public final class LlmException extends ShishoException {

    public LlmException(String messageKey, Object... arguments) {
        super(messageKey, null, arguments);
    }

    public LlmException(String messageKey, Throwable cause, Object... arguments) {
        super(messageKey, cause, arguments);
    }
}
