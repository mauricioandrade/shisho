package dev.mauricioandrade.shisho.infrastructure.llm;

import com.sun.net.httpserver.HttpServer;
import dev.mauricioandrade.shisho.domain.exception.LlmException;
import dev.mauricioandrade.shisho.infrastructure.json.Json;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** Against the JDK's HttpServer playing Ollama: no test needs a model installed. */
class OllamaLlmProviderTest {

    private final AtomicReference<String> request = new AtomicReference<>();
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void streamsTheAnswerFragmentByFragment() {
        String url = ollama(200, """
                {"model":"m","message":{"role":"assistant","content":"Injeção"},"done":false}
                {"model":"m","message":{"role":"assistant","content":" em campo"},"done":false}

                {"model":"m","message":{"role":"assistant","content":""},"done":true,"total_duration":1}
                """);
        List<String> fragments = new ArrayList<>();

        new OllamaLlmProvider(url, "gemma3:4b").chat("regras", "fatos", fragments::add);

        assertThat(fragments).containsExactly("Injeção", " em campo", "");
    }

    @Test
    void sendsTheModelBothMessagesAndTheContextWindow() {
        String url = ollama(200, "{\"done\":true}\n");

        new OllamaLlmProvider(url + "/", "gemma3:4b").chat("regras", "fatos", fragment -> { });

        assertThat(Json.read(request.get())).isEqualTo(Map.of(
                "model", "gemma3:4b",
                "messages", List.of(Map.of("role", "system", "content", "regras"), Map.of("role", "user", "content", "fatos")),
                "stream", true,
                "options", Map.of("num_ctx", 8192, "temperature", 0.2)));
    }

    @Test
    void explainsHowToInstallAMissingModel() {
        String url = ollama(404, "{\"error\":\"model 'qwen' not found\"}");

        assertThatExceptionOfType(LlmException.class)
                .isThrownBy(() -> new OllamaLlmProvider(url, "qwen").chat("regras", "fatos", fragment -> { }))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("llm.error.model-not-found"))
                .satisfies(e -> assertThat(e.arguments()).containsExactly("qwen"));
    }

    @Test
    void doesNotBlameTheModelForAServerThatIsNotOllama() {
        String url = ollama(404, "<html>Not Found</html>");

        assertThatExceptionOfType(LlmException.class)
                .isThrownBy(() -> new OllamaLlmProvider(url, "m").chat("regras", "fatos", fragment -> { }))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("llm.error.server"))
                .satisfies(e -> assertThat(e.arguments()).containsExactly("HTTP 404"));
    }

    @Test
    void reportsAnErrorInTheMiddleOfTheStream() {
        String url = ollama(200, """
                {"message":{"content":"Começo"},"done":false}
                {"error":"out of memory"}
                """);
        List<String> fragments = new ArrayList<>();

        assertThatExceptionOfType(LlmException.class)
                .isThrownBy(() -> new OllamaLlmProvider(url, "m").chat("regras", "fatos", fragments::add))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("llm.error.server"))
                .satisfies(e -> assertThat(e.arguments()).containsExactly("out of memory"));
        assertThat(fragments).containsExactly("Começo");
    }

    @Test
    void rejectsAnAnswerThatIsNotOllamas() {
        String url = ollama(200, "isto não é json {\n");

        assertThatExceptionOfType(LlmException.class)
                .isThrownBy(() -> new OllamaLlmProvider(url, "m").chat("regras", "fatos", fragment -> { }))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("llm.error.response"));
    }

    @Test
    void saysWhereItLookedWhenNothingAnswers() {
        String url = ollama(200, "");
        server.stop(0);
        server = null;

        assertThatExceptionOfType(LlmException.class)
                .isThrownBy(() -> new OllamaLlmProvider(url, "m").chat("regras", "fatos", fragment -> { }))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("llm.error.unreachable"))
                .satisfies(e -> assertThat(e.arguments()).containsExactly(url));
    }

    @Test
    void saysWhereItLookedWhenTheUrlIsInvalid() {
        assertThatExceptionOfType(LlmException.class)
                .isThrownBy(() -> new OllamaLlmProvider("localhost:11434", "m").chat("regras", "fatos", fragment -> { }))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("llm.error.unreachable"))
                .satisfies(e -> assertThat(e.arguments()).containsExactly("localhost:11434"));
    }

    /** Starts a fake Ollama answering {@code /api/chat} with {@code status} and {@code body}; returns its URL. */
    private String ollama(int status, String body) {
        try {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.createContext("/api/chat", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
            try (OutputStream response = exchange.getResponseBody()) {
                response.write(bytes);
            }
        });
        server.start();
        return "http://" + server.getAddress().getHostString() + ":" + server.getAddress().getPort();
    }
}
