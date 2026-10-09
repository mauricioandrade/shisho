package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.exception.AnalysisException;
import dev.mauricioandrade.shisho.domain.exception.LlmException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.domain.port.out.LlmProvider;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.llm.SenseiPrompt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AskCommandTest {

    private static final Messages MESSAGES = new Messages(Messages.DEFAULT_LOCALE);

    @TempDir
    Path project;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final List<String> calls = new ArrayList<>();

    @Test
    void streamsTheExplanationAndRemindsWhereTheFactsComeFrom() {
        LlmProvider llm = (instructions, prompt, answer) -> {
            calls.add(prompt);
            answer.accept("O veredito ");
            answer.accept("é claro.");
        };

        assertThat(run(root -> analysis(), llm)).isZero();

        assertThat(text(out)).isEqualTo("""
                O veredito é claro.

                Explicação escrita por gemma3:4b, que pode errar. O veredito, a graduação e as lições vêm da análise \
                determinística: shisho analyze.
                """);
        assertThat(text(err)).isEqualTo("Perguntando ao gemma3:4b em http://localhost:11434...\n");
        assertThat(calls).singleElement().asString()
                .startsWith("師匠 Shishō — análise de loja  (Java 21)\n")
                .endsWith("\nExplique o veredito e a graduação deste projeto: o que significam, quais lições pesam mais e por "
                        + "onde o autor deveria começar.\n");
    }

    @Test
    void passesTheQuestionTheServerAndTheModelOn() {
        List<String> created = new ArrayList<>();
        LlmProviderFactory factory = (url, model) -> {
            created.add(url + " " + model);
            return (instructions, prompt, answer) -> {
                calls.add(prompt);
                answer.accept("Resposta.\n");
            };
        };

        assertThat(run(root -> analysis(), factory, "--model", "qwen2.5:7b", "--url", "http://gpu:11434",
                project.toString(), "por", "que", "layered?")).isZero();

        assertThat(created).containsExactly("http://gpu:11434 qwen2.5:7b");
        assertThat(calls).singleElement().asString().endsWith("\n## Pergunta\n\npor que layered?\n");
        assertThat(text(out)).startsWith("Resposta.\n\nExplicação escrita por qwen2.5:7b");
    }

    @Test
    void exitsWithThreeAndExplainsWhenTheModelIsMissing() {
        LlmProvider llm = (instructions, prompt, answer) -> {
            throw new LlmException("llm.error.model-not-found", "gemma3:4b");
        };

        assertThat(run(root -> analysis(), llm)).isEqualTo(3);

        assertThat(text(out)).isEmpty();
        assertThat(text(err)).endsWith("O modelo gemma3:4b não está instalado no Ollama. Instale com: ollama pull gemma3:4b\n");
    }

    @Test
    void endsThePartialAnswerBeforeTheError() {
        LlmProvider llm = (instructions, prompt, answer) -> {
            answer.accept("Começo da resposta");
            throw new LlmException("llm.error.server", "out of memory");
        };

        assertThat(run(root -> analysis(), llm)).isEqualTo(3);

        assertThat(text(out)).isEqualTo("Começo da resposta\n");
        assertThat(text(err)).endsWith("O Ollama respondeu com um erro: out of memory\n");
    }

    @Test
    void neverCallsTheModelWhenTheProjectCannotBeAnalyzed() {
        AnalyzeProject broken = root -> {
            throw new AnalysisException("analysis.error.unsupported-java-version", 17, 21);
        };

        assertThat(run(broken, (instructions, prompt, answer) -> calls.add(prompt))).isEqualTo(3);

        assertThat(calls).isEmpty();
        assertThat(text(err)).isEqualTo(MESSAGES.text("analysis.error.unsupported-java-version", 17, 21) + "\n");
    }

    @Test
    void exitsWithTwoForAPathThatIsNotADirectory() {
        assertThat(run(root -> analysis(), (url, model) -> (instructions, prompt, answer) -> calls.add(prompt),
                project.resolve("nao-existe").toString())).isEqualTo(2);

        assertThat(calls).isEmpty();
    }

    private int run(AnalyzeProject analyzeProject, LlmProvider llm) {
        return run(analyzeProject, (url, model) -> llm, project.toString());
    }

    private int run(AnalyzeProject analyzeProject, LlmProviderFactory llmProviders, String... arguments) {
        AskCommand command = new AskCommand(analyzeProject, new SenseiPrompt(MESSAGES, List.of()), llmProviders, MESSAGES,
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        return new CommandLine(command).setResourceBundle(MESSAGES.bundle()).execute(arguments);
    }

    private Analysis analysis() {
        return new Analysis(project, "loja", new JavaVersion(21), true, Optional.empty(), Optional.empty(), List.of(), List.of(), 100,
                Rank.DAN_1);
    }

    private static String text(ByteArrayOutputStream stream) {
        return stream.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }
}
