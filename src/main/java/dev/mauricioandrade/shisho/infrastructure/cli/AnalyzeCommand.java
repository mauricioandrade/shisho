package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.exception.ShishoException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * {@code shisho analyze [--format terminal|sarif|html] [--output <file>] [--sensei] [--shoshin] <dir>}: the exit code tells CI
 * whether there is a GRAVE lesson, whatever the format.
 */
@Command(name = "analyze", mixinStandardHelpOptions = true, exitCodeOnExecutionException = AnalyzeCommand.EXIT_NOT_ANALYZED)
public final class AnalyzeCommand implements Callable<Integer> {

    public static final int EXIT_GRAVE_LESSONS = 1;
    public static final int EXIT_INVALID_INPUT = ExitCode.USAGE;
    public static final int EXIT_NOT_ANALYZED = 3;

    private final AnalyzeProject analyzeProject;
    private final ReportWriterFactory reportWriters;
    private final Messages messages;
    private final PrintStream out;
    private final PrintStream err;

    @Parameters(index = "0", paramLabel = "<dir>", descriptionKey = "analyze.project")
    private Path project;

    @Option(names = "--format", paramLabel = "<formato>", defaultValue = "terminal", descriptionKey = "analyze.format")
    private ReportFormat format;

    @Option(names = {"-o", "--output"}, paramLabel = "<arquivo>", descriptionKey = "analyze.output")
    private Path output;

    @Option(names = "--sensei", descriptionKey = "analyze.sensei")
    private boolean sensei;

    @Option(names = "--shoshin", descriptionKey = "analyze.shoshin")
    private boolean shoshin;

    public AnalyzeCommand(
            AnalyzeProject analyzeProject, ReportWriterFactory reportWriters, Messages messages, PrintStream out, PrintStream err) {
        this.analyzeProject = analyzeProject;
        this.reportWriters = reportWriters;
        this.messages = messages;
        this.out = out;
        this.err = err;
    }

    @Override
    public Integer call() {
        if (!Files.isDirectory(project)) {
            err.println(messages.text("cli.error.not-a-directory", project.toString()));
            return EXIT_INVALID_INPUT;
        }
        Analysis analysis;
        try {
            analysis = analyzeProject.analyze(project);
        } catch (ShishoException e) {
            err.println(messages.text(e.messageKey(), e.arguments().toArray()));
            return EXIT_NOT_ANALYZED;
        }
        if (!write(analysis)) {
            return EXIT_INVALID_INPUT;
        }
        return analysis.count(Severity.GRAVE) > 0 ? EXIT_GRAVE_LESSONS : ExitCode.OK;
    }

    private boolean write(Analysis analysis) {
        if (output == null) {
            reportWriters.create(format, out, sensei, shoshin).write(analysis);
            return true;
        }
        try (PrintStream file = new PrintStream(Files.newOutputStream(output), false, StandardCharsets.UTF_8)) {
            reportWriters.create(format, file, sensei, shoshin).write(analysis);
            if (!file.checkError()) {
                return true;
            }
        } catch (IOException ignored) {
            // reported below, like a write error PrintStream swallowed
        }
        err.println(messages.text("cli.error.cannot-write", output.toString()));
        return false;
    }
}
