package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.exception.ShishoException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.json.Json;
import dev.mauricioandrade.shisho.infrastructure.report.TerminalReportWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * {@code shisho claude-hook}: the Claude Code plugin's PostToolUse hook. It reads the hook's JSON
 * from standard input, analyzes the project of the edited file and shows Claude that file's
 * lessons on standard error with exit code 2, the code that makes Claude Code show it to Claude.
 * Anything else, a file outside a Maven project included, exits 0 in silence. See
 * docs/adr/0008-claude-code-plugin.md.
 */
@Command(name = "claude-hook", hidden = true)
public final class ClaudeHookCommand implements Callable<Integer> {

    /** PostToolUse: exit code 2 shows standard error to Claude. */
    static final int EXIT_SHOW_CLAUDE = 2;
    private static final String BUILD_FILE = "pom.xml";

    private final AnalyzeProject analyzeProject;
    private final Messages messages;
    private final InputStream in;
    private final PrintStream err;

    public ClaudeHookCommand(AnalyzeProject analyzeProject, Messages messages, InputStream in, PrintStream err) {
        this.analyzeProject = analyzeProject;
        this.messages = messages;
        this.in = in;
        this.err = err;
    }

    @Override
    public Integer call() {
        Optional<Path> edited = editedFile();
        Optional<Path> root = edited.flatMap(ClaudeHookCommand::projectRoot);
        if (root.isEmpty()) {
            return ExitCode.OK;
        }
        Analysis analysis;
        try {
            analysis = analyzeProject.analyze(root.get());
        } catch (ShishoException e) {
            return ExitCode.OK;
        }
        Path file = root.get().relativize(edited.get());
        List<Lesson> lessons = analysis.lessons().stream()
                .filter(lesson -> lesson.file().equals(file) && lesson.severity() != Severity.INFO)
                .toList();
        if (lessons.isEmpty()) {
            return ExitCode.OK;
        }
        err.println(messages.text("hook.header", lessons.size(), file.getFileName().toString()));
        err.println();
        new TerminalReportWriter(err, messages, List.of()).writeLessons(lessons);
        err.println();
        TerminalReportWriter.wrap(messages.text("hook.footer", root.get().toString()), TerminalReportWriter.WIDTH)
                .forEach(err::println);
        return EXIT_SHOW_CLAUDE;
    }

    /** {@code tool_input.file_path}, absolute or relative to {@code cwd}; empty if the JSON has neither. */
    private Optional<Path> editedFile() {
        Object hook;
        try {
            hook = Json.read(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | IllegalArgumentException e) {
            return Optional.empty();
        }
        if (!(hook instanceof Map<?, ?> fields) || !(fields.get("tool_input") instanceof Map<?, ?> input)
                || !(input.get("file_path") instanceof String filePath)) {
            return Optional.empty();
        }
        Path file = Path.of(filePath);
        if (!file.isAbsolute() && fields.get("cwd") instanceof String cwd) {
            file = Path.of(cwd).resolve(file);
        }
        return Optional.of(file.toAbsolutePath().normalize());
    }

    /** The nearest directory above the file with a pom.xml. */
    private static Optional<Path> projectRoot(Path file) {
        for (Path directory = file.getParent(); directory != null; directory = directory.getParent()) {
            if (Files.isRegularFile(directory.resolve(BUILD_FILE))) {
                return Optional.of(directory);
            }
        }
        return Optional.empty();
    }
}
