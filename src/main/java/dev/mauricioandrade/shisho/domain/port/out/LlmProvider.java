package dev.mauricioandrade.shisho.domain.port.out;

import java.util.function.Consumer;

/**
 * A language model that turns facts already computed into prose. It never decides anything: the
 * lessons, the verdict and the rank are in the prompt before it is called.
 */
public interface LlmProvider {

    /**
     * Streams the answer to {@code answer}, fragment by fragment, as the model writes it.
     *
     * @param instructions the role and the rules the model must follow
     * @param prompt       the facts and the question
     * @throws dev.mauricioandrade.shisho.domain.exception.LlmException if the model cannot be
     *         reached or is not installed
     */
    void chat(String instructions, String prompt, Consumer<String> answer);
}
