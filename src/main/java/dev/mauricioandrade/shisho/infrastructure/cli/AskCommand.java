package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.exception.ShishoException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.llm.OllamaLlmProvider;
import dev.mauricioandrade.shisho.infrastructure.llm.SenseiPrompt;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * {@code shisho ask [--model <m>] [--url <u>] <dir> [pergunta...]}: a local LLM explains the
 * analysis, streaming its answer. The answer goes to stdout; progress and errors go to stderr.
 * See docs/adr/0004-local-llm.md.
 */
@Command(name = "ask", mixinStandardHelpOptions = true, exitCodeOnExecutionException = AnalyzeCommand.EXIT_NOT_ANALYZED)
public final class AskCommand implements Callable<Integer> {

    private final AnalyzeProject analyzeProject;
    private final SenseiPrompt prompt;
    private final LlmProviderFactory llmProviders;
    private final Messages messages;
    private final PrintStream out;
    private final PrintStream err;

    @Parameters(index = "0", paramLabel = "<dir>", descriptionKey = "ask.project")
    private Path project;

    @Parameters(index = "1..*", paramLabel = "<pergunta>", arity = "0..*", descriptionKey = "ask.question")
    private List<String> question;

    @Option(names = "--model", paramLabel = "<modelo>", defaultValue = OllamaLlmProvider.DEFAULT_MODEL, descriptionKey = "ask.model")
    private String model;

    @Option(names = "--url", paramLabel = "<url>", defaultValue = OllamaLlmProvider.DEFAULT_URL, descriptionKey = "ask.url")
    private String url;

    public AskCommand(AnalyzeProject analyzeProject, SenseiPrompt prompt, LlmProviderFactory llmProviders, Messages messages,
                      PrintStream out, PrintStream err) {
        this.analyzeProject = analyzeProject;
        this.prompt = prompt;
        this.llmProviders = llmProviders;
        this.messages = messages;
        this.out = out;
        this.err = err;
    }

    @Override
    public Integer call() {
        if (!Files.isDirectory(project)) {
            err.println(messages.text("cli.error.not-a-directory", project.toString()));
            return AnalyzeCommand.EXIT_INVALID_INPUT;
        }
        Answer answer = new Answer();
        try {
            Analysis analysis = analyzeProject.analyze(project);
            String facts = prompt.facts(analysis, Optional.ofNullable(question).map(words -> String.join(" ", words)));
            err.println(messages.text("ask.waiting", model, url));
            llmProviders.create(url, model).chat(prompt.instructions(), facts, answer::print);
        } catch (ShishoException e) {
            answer.endLine();
            err.println(messages.text(e.messageKey(), e.arguments().toArray()));
            return AnalyzeCommand.EXIT_NOT_ANALYZED;
        }
        answer.endLine();
        out.println();
        out.println(messages.text("ask.footer", model));
        return ExitCode.OK;
    }

    /** The streamed text, so the next message starts on a line of its own. */
    private final class Answer {

        private boolean openLine;

        void print(String fragment) {
            if (!fragment.isEmpty()) {
                out.print(fragment);
                out.flush();
                openLine = !fragment.endsWith("\n");
            }
        }

        void endLine() {
            if (openLine) {
                out.println();
                openLine = false;
            }
        }
    }
}
