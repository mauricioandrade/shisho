package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.exception.ShishoException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.domain.port.out.ReportWriter;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/** {@code shisho analyze <dir>}: the exit code tells CI whether there is a GRAVE lesson. */
@Command(name = "analyze", mixinStandardHelpOptions = true, exitCodeOnExecutionException = AnalyzeCommand.EXIT_NOT_ANALYZED)
public final class AnalyzeCommand implements Callable<Integer> {

    public static final int EXIT_GRAVE_LESSONS = 1;
    public static final int EXIT_INVALID_INPUT = ExitCode.USAGE;
    public static final int EXIT_NOT_ANALYZED = 3;

    private final AnalyzeProject analyzeProject;
    private final ReportWriter reportWriter;
    private final Messages messages;
    private final PrintStream err;

    @Parameters(index = "0", paramLabel = "<dir>", descriptionKey = "analyze.project")
    private Path project;

    public AnalyzeCommand(AnalyzeProject analyzeProject, ReportWriter reportWriter, Messages messages, PrintStream err) {
        this.analyzeProject = analyzeProject;
        this.reportWriter = reportWriter;
        this.messages = messages;
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
        reportWriter.write(analysis);
        return analysis.count(Severity.GRAVE) > 0 ? EXIT_GRAVE_LESSONS : ExitCode.OK;
    }
}
