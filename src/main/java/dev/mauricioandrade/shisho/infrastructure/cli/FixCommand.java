package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.exception.ShishoException;
import dev.mauricioandrade.shisho.domain.model.FileFix;
import dev.mauricioandrade.shisho.domain.model.FixPlan;
import dev.mauricioandrade.shisho.domain.port.in.FixProject;
import dev.mauricioandrade.shisho.infrastructure.fix.LineDiff;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * {@code shisho fix [--apply] <dir>}: shows the diff of the safe corrections; {@code --apply}
 * writes them, file by file, only if the file did not change since it was analyzed.
 */
@Command(name = "fix", mixinStandardHelpOptions = true, exitCodeOnExecutionException = AnalyzeCommand.EXIT_NOT_ANALYZED)
public final class FixCommand implements Callable<Integer> {

    private final FixProject fixProject;
    private final Messages messages;
    private final PrintStream out;
    private final PrintStream err;

    @Parameters(index = "0", paramLabel = "<dir>", descriptionKey = "fix.project")
    private Path project;

    @Option(names = "--apply", descriptionKey = "fix.apply")
    private boolean apply;

    public FixCommand(FixProject fixProject, Messages messages, PrintStream out, PrintStream err) {
        this.fixProject = fixProject;
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
        FixPlan plan;
        try {
            plan = fixProject.plan(project);
        } catch (ShishoException e) {
            err.println(messages.text(e.messageKey(), e.arguments().toArray()));
            return AnalyzeCommand.EXIT_NOT_ANALYZED;
        }
        List<FileFix> fixes = plan.fixes();
        boolean complete = true;
        if (apply) {
            for (FileFix fix : fixes) {
                complete &= write(fix);
            }
        } else {
            fixes.forEach(fix -> out.print(LineDiff.unified(slashes(fix.file()), fix.original(), fix.fixed())));
        }
        summary(plan);
        return complete ? ExitCode.OK : AnalyzeCommand.EXIT_NOT_ANALYZED;
    }

    private boolean write(FileFix fix) {
        Path target = project.resolve(fix.file());
        try {
            if (!Files.readString(target).equals(fix.original())) {
                err.println(messages.text("fix.error.changed", slashes(fix.file())));
                return false;
            }
            Files.writeString(target, fix.fixed());
            return true;
        } catch (IOException e) {
            err.println(messages.text("fix.error.write", slashes(fix.file())));
            return false;
        }
    }

    private void summary(FixPlan plan) {
        if (plan.analysis().lessons().isEmpty()) {
            out.println(messages.text("fix.summary.clean"));
            return;
        }
        int lessons = plan.fixes().stream().mapToInt(fix -> fix.lessons().size()).sum();
        if (lessons == 0) {
            out.println(messages.text("fix.summary.nothing"));
        } else {
            out.println(messages.text(apply ? "fix.summary.applied" : "fix.summary.preview",
                    messages.text("fix.count.lessons", lessons), messages.text("fix.count.files", plan.fixes().size())));
        }
        int unfixed = plan.unfixed().size();
        if (unfixed > 0) {
            out.println(messages.text("fix.summary.unfixed", messages.text("fix.count.lessons", unfixed)));
        }
    }

    private static String slashes(Path file) {
        return StreamSupport.stream(file.spliterator(), false).map(Path::toString).collect(Collectors.joining("/"));
    }
}
