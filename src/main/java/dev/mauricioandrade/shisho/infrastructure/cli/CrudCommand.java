package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.exception.ScaffoldException;
import dev.mauricioandrade.shisho.domain.exception.ShishoException;
import dev.mauricioandrade.shisho.domain.model.CrudPlan;
import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.GeneratedFile;
import dev.mauricioandrade.shisho.domain.port.in.GenerateCrud;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.report.TerminalReportWriter;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * {@code shisho crud [--arquitetura <id>] [--pacote <p>] [--rota <r>] <dir> <Entidade> <campo:Tipo>...}:
 * creates a CRUD in the project's architecture and explains every file. It never overwrites:
 * if a file already exists, nothing is written. See docs/adr/0005-crud-scaffolding.md.
 */
@Command(name = "crud", mixinStandardHelpOptions = true, exitCodeOnExecutionException = AnalyzeCommand.EXIT_NOT_ANALYZED)
public final class CrudCommand implements Callable<Integer> {

    private static final String INDENT = "  ";

    private final GenerateCrud generateCrud;
    private final Messages messages;
    private final PrintStream out;
    private final PrintStream err;

    @Parameters(index = "0", paramLabel = "<dir>", descriptionKey = "crud.project")
    private Path project;

    @Parameters(index = "1", paramLabel = "<Entidade>", descriptionKey = "crud.entity")
    private String entity;

    @Parameters(index = "2..*", paramLabel = "<campo:Tipo>", arity = "0..*", descriptionKey = "crud.fields")
    private List<String> fields;

    @Option(names = "--arquitetura", paramLabel = "<id>", descriptionKey = "crud.architecture")
    private String architecture;

    @Option(names = "--pacote", paramLabel = "<pacote>", descriptionKey = "crud.package")
    private String basePackage;

    @Option(names = "--rota", paramLabel = "<rota>", descriptionKey = "crud.route")
    private String route;

    public CrudCommand(GenerateCrud generateCrud, Messages messages, PrintStream out, PrintStream err) {
        this.generateCrud = generateCrud;
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
        CrudPlan plan;
        try {
            plan = generateCrud.plan(project, new CrudRequest(entity, fields(),
                    Optional.ofNullable(architecture), Optional.ofNullable(basePackage), Optional.ofNullable(route)));
        } catch (ScaffoldException e) {
            err.println(messages.text(e.messageKey(), e.arguments().toArray()));
            return AnalyzeCommand.EXIT_INVALID_INPUT;
        } catch (ShishoException e) {
            err.println(messages.text(e.messageKey(), e.arguments().toArray()));
            return AnalyzeCommand.EXIT_NOT_ANALYZED;
        }
        List<GeneratedFile> existing = plan.files().stream().filter(file -> Files.exists(project.resolve(file.file()))).toList();
        if (!existing.isEmpty()) {
            existing.forEach(file -> err.println(messages.text("crud.error.exists", slashes(file.file()))));
            err.println(messages.text("crud.error.nothing-written"));
            return AnalyzeCommand.EXIT_INVALID_INPUT;
        }
        for (GeneratedFile file : plan.files()) {
            if (!write(file)) {
                return AnalyzeCommand.EXIT_NOT_ANALYZED;
            }
        }
        report(plan);
        return ExitCode.OK;
    }

    /** {@code nome:String} becomes a field; the renderer checks the name and the type. */
    private List<CrudRequest.Field> fields() {
        List<CrudRequest.Field> parsed = new ArrayList<>();
        for (String field : fields == null ? List.<String>of() : fields) {
            int colon = field.indexOf(':');
            if (colon <= 0 || colon == field.length() - 1) {
                throw new ScaffoldException("crud.error.field", field);
            }
            parsed.add(new CrudRequest.Field(field.substring(0, colon), field.substring(colon + 1)));
        }
        return parsed;
    }

    private boolean write(GeneratedFile file) {
        Path target = project.resolve(file.file());
        try {
            Files.createDirectories(target.getParent());
            Files.writeString(target, file.content());
            return true;
        } catch (IOException e) {
            err.println(messages.text("fix.error.write", slashes(file.file())));
            return false;
        }
    }

    private void report(CrudPlan plan) {
        out.println(messages.text("crud.header", entity, plan.architecture().name()));
        out.println();
        plan.files().forEach(file -> describe("+ ", file.file(), file.explanation()));
        plan.skipped().forEach(file -> describe("= ", file.file(), messages.text("crud.skipped")));
        plan.otherArchitecture().ifPresent(other -> warn(messages.text("crud.warning.mixed", other.name(), plan.architecture().name())));
        if (!plan.missingDependencies().isEmpty()) {
            warn(messages.text("crud.warning.dependencies", plan.missingDependencies().stream()
                    .map(alternatives -> String.join("/", alternatives))
                    .collect(Collectors.joining(", "))));
        }
        out.println(messages.text("crud.summary", plan.files().size(), messages.text("fix.count.files", plan.files().size())));
    }

    private void describe(String symbol, Path file, String explanation) {
        out.println(symbol + slashes(file));
        TerminalReportWriter.wrap(explanation, TerminalReportWriter.WIDTH - INDENT.length()).forEach(line -> out.println(INDENT + line));
        out.println();
    }

    private void warn(String warning) {
        TerminalReportWriter.wrap(warning, TerminalReportWriter.WIDTH).forEach(out::println);
        out.println();
    }

    private static String slashes(Path file) {
        return StreamSupport.stream(file.spliterator(), false).map(Path::toString).collect(Collectors.joining("/"));
    }
}
