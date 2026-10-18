package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.exception.ShishoException;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.port.in.RecordBaseline;
import dev.mauricioandrade.shisho.domain.port.in.RecordBaseline.Recorded;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * {@code shisho baseline <dir>}: records the project's lessons, so that {@code shisho analyze}
 * shows and fails only on new ones. Recording never fails because of a GRAVE lesson.
 */
@Command(name = "baseline", mixinStandardHelpOptions = true, exitCodeOnExecutionException = AnalyzeCommand.EXIT_NOT_ANALYZED)
public final class BaselineCommand implements Callable<Integer> {

    private final RecordBaseline recordBaseline;
    private final Messages messages;
    private final PrintStream out;
    private final PrintStream err;

    @Parameters(index = "0", paramLabel = "<dir>", descriptionKey = "baseline.project")
    private Path project;

    public BaselineCommand(RecordBaseline recordBaseline, Messages messages, PrintStream out, PrintStream err) {
        this.recordBaseline = recordBaseline;
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
        Recorded recorded;
        try {
            recorded = recordBaseline.record(project);
        } catch (ShishoException e) {
            err.println(messages.text(e.messageKey(), e.arguments().toArray()));
            return AnalyzeCommand.EXIT_NOT_ANALYZED;
        }
        long graves = recorded.lessons().stream().filter(lesson -> lesson.severity() == Severity.GRAVE).count();
        out.println(messages.text("baseline.recorded", recorded.file().toString().replace('\\', '/'), recorded.lessons().size(), graves));
        return ExitCode.OK;
    }
}
