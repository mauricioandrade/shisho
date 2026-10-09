package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.port.out.LlmProvider;

/** The server and the model are only known once the command line is parsed. */
@FunctionalInterface
public interface LlmProviderFactory {

    LlmProvider create(String url, String model);
}
