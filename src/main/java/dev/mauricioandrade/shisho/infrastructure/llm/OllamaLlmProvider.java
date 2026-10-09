package dev.mauricioandrade.shisho.infrastructure.llm;

import dev.mauricioandrade.shisho.domain.exception.LlmException;
import dev.mauricioandrade.shisho.domain.port.out.LlmProvider;
import dev.mauricioandrade.shisho.infrastructure.json.Json;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Talks to a local Ollama through {@code POST /api/chat}, streaming the answer line by line. See
 * docs/adr/0004-local-llm.md.
 */
public final class OllamaLlmProvider implements LlmProvider {

    public static final String DEFAULT_URL = "http://localhost:11434";
    public static final String DEFAULT_MODEL = "gemma3:4b";

    /** Ollama silently drops the start of a longer prompt, and the start holds the rules. */
    static final int CONTEXT_TOKENS = 8192;
    /** Low enough to stick to the facts; zero makes small models repeat themselves. */
    static final double TEMPERATURE = 0.2;
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final int NOT_FOUND = 404;

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(CONNECT_TIMEOUT)
            .build();
    private final String url;
    private final String model;

    /** @param url where Ollama listens, such as {@value #DEFAULT_URL} */
    public OllamaLlmProvider(String url, String model) {
        this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.model = model;
    }

    @Override
    public void chat(String instructions, String prompt, Consumer<String> answer) {
        HttpResponse<Stream<String>> response = send(Json.write(body(instructions, prompt)));
        try (Stream<String> lines = response.body()) {
            if (response.statusCode() != 200) {
                Optional<String> error = error(lines.collect(Collectors.joining()));
                throw response.statusCode() == NOT_FOUND && error.isPresent()
                        ? new LlmException("llm.error.model-not-found", model)
                        : new LlmException("llm.error.server", error.orElse("HTTP " + response.statusCode()));
            }
            lines.filter(line -> !line.isBlank()).forEach(line -> answer.accept(fragment(line)));
        } catch (UncheckedIOException e) {
            throw new LlmException("llm.error.unreachable", e, url);
        }
    }

    private Map<String, Object> body(String instructions, String prompt) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", instructions),
                        Map.of("role", "user", "content", prompt)),
                "stream", true,
                "options", Map.of("num_ctx", CONTEXT_TOKENS, "temperature", TEMPERATURE));
    }

    private HttpResponse<Stream<String>> send(String body) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url + "/api/chat"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.ofLines());
        } catch (IOException | IllegalArgumentException e) {
            throw new LlmException("llm.error.unreachable", e, url);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("llm.error.unreachable", e, url);
        }
    }

    /** One line of the stream: {@code {"message":{"content":"..."},"done":false}} or {@code {"error":"..."}}. */
    private static String fragment(String line) {
        Object chunk = read(line);
        if (chunk instanceof Map<?, ?> map && map.get("error") instanceof String error) {
            throw new LlmException("llm.error.server", error);
        }
        if (chunk instanceof Map<?, ?> map && map.get("message") instanceof Map<?, ?> message) {
            return message.get("content") instanceof String content ? content : "";
        }
        if (chunk instanceof Map<?, ?> map && Boolean.TRUE.equals(map.get("done"))) {
            return "";
        }
        throw new LlmException("llm.error.response");
    }

    /** Ollama explains a failed request as {@code {"error":"..."}}; anything else is not Ollama talking. */
    private static Optional<String> error(String body) {
        try {
            return Json.read(body) instanceof Map<?, ?> map && map.get("error") instanceof String error
                    ? Optional.of(error) : Optional.empty();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static Object read(String json) {
        try {
            return Json.read(json);
        } catch (IllegalArgumentException e) {
            throw new LlmException("llm.error.response", e);
        }
    }
}
