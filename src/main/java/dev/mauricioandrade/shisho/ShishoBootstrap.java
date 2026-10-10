package dev.mauricioandrade.shisho;

import dev.mauricioandrade.shisho.application.usecase.AnalyzeProjectService;
import dev.mauricioandrade.shisho.application.usecase.ClassifyArchitectureService;
import dev.mauricioandrade.shisho.application.usecase.FixProjectService;
import dev.mauricioandrade.shisho.application.usecase.GenerateCrudService;
import dev.mauricioandrade.shisho.application.usecase.RankCalculator;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.domain.port.in.ClassifyArchitecture;
import dev.mauricioandrade.shisho.domain.port.in.FixProject;
import dev.mauricioandrade.shisho.domain.port.in.GenerateCrud;
import dev.mauricioandrade.shisho.infrastructure.build.MavenBuildFileReader;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import dev.mauricioandrade.shisho.infrastructure.fix.JavaParserSourceFixer;
import dev.mauricioandrade.shisho.infrastructure.cli.AnalyzeCommand;
import dev.mauricioandrade.shisho.infrastructure.cli.ClaudeHookCommand;
import dev.mauricioandrade.shisho.infrastructure.cli.AskCommand;
import dev.mauricioandrade.shisho.infrastructure.cli.CrudCommand;
import dev.mauricioandrade.shisho.infrastructure.cli.FixCommand;
import dev.mauricioandrade.shisho.infrastructure.cli.ReportWriterFactory;
import dev.mauricioandrade.shisho.infrastructure.cli.ShishoCommand;
import dev.mauricioandrade.shisho.infrastructure.config.YamlProjectConfigReader;
import dev.mauricioandrade.shisho.infrastructure.i18n.Language;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.kata.Katas;
import dev.mauricioandrade.shisho.infrastructure.llm.OllamaLlmProvider;
import dev.mauricioandrade.shisho.infrastructure.llm.SenseiPrompt;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import dev.mauricioandrade.shisho.infrastructure.report.HtmlReportWriter;
import dev.mauricioandrade.shisho.infrastructure.report.SarifReportWriter;
import dev.mauricioandrade.shisho.infrastructure.report.TerminalReportWriter;
import dev.mauricioandrade.shisho.infrastructure.scaffold.TemplateScaffoldRenderer;
import picocli.CommandLine;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Composition root: builds the object graph by hand and runs the command line. */
public final class ShishoBootstrap {

    private ShishoBootstrap() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.getenv(Language.VARIABLE), System.in, new FileOutputStream(FileDescriptor.out),
                new FileOutputStream(FileDescriptor.err)));
    }

    /** In Portuguese, whatever the environment says: what the tests need. */
    static int run(String[] args, OutputStream stdout, OutputStream stderr) {
        return run(args, null, InputStream.nullInputStream(), stdout, stderr);
    }

    /** In {@code languageCode}, with nothing on standard input. */
    static int run(String[] args, String languageCode, OutputStream stdout, OutputStream stderr) {
        return run(args, languageCode, InputStream.nullInputStream(), stdout, stderr);
    }

    /** Output is always UTF-8: the report has 師匠, accents and box symbols. {@code languageCode}: {@code SHISHO_IDIOMA}. */
    static int run(String[] args, String languageCode, InputStream stdin, OutputStream stdout, OutputStream stderr) {
        PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8);
        Optional<Language> language = Language.of(languageCode);
        if (language.isEmpty()) {
            err.println(new Messages(Messages.DEFAULT_LOCALE).text("cli.error.language", Language.VARIABLE, languageCode));
            return AnalyzeCommand.EXIT_INVALID_INPUT;
        }
        Messages messages = new Messages(language.get().locale());

        MarkdownCatalogRepository catalog = MarkdownCatalogRepository.bundled(language.get().catalogTranslation());
        ClassifyArchitecture classifyArchitecture = new ClassifyArchitectureService(catalog, Detectors.all());
        AnalyzeProject analyzeProject = new AnalyzeProjectService(
                new MavenBuildFileReader(),
                new YamlProjectConfigReader(),
                new JavaParserSourceParser(),
                Katas.all(catalog.avoidedDependencies()),
                classifyArchitecture,
                new RankCalculator());

        ReportWriterFactory reportWriters = (format, destination, sensei) -> switch (format) {
            case TERMINAL -> new TerminalReportWriter(destination, messages, sensei ? catalog.kataGuides() : List.of());
            case SARIF -> new SarifReportWriter(destination, messages, catalog.kataGuides(), ShishoCommand.version(), Path.of(""));
            case HTML -> new HtmlReportWriter(destination, messages, catalog.kataGuides(), ShishoCommand.version());
        };

        FixProject fixProject = new FixProjectService(analyzeProject, new JavaParserSourceFixer());
        GenerateCrud generateCrud = new GenerateCrudService(new MavenBuildFileReader(), new YamlProjectConfigReader(),
                new JavaParserSourceParser(), classifyArchitecture, catalog, new TemplateScaffoldRenderer());

        CommandLine commandLine = new CommandLine(new ShishoCommand())
                .addSubcommand(new AnalyzeCommand(analyzeProject, reportWriters, messages, out, err))
                .addSubcommand(new FixCommand(fixProject, messages, out, err))
                .addSubcommand(new AskCommand(analyzeProject, new SenseiPrompt(messages, catalog.architectures()),
                        OllamaLlmProvider::new, messages, out, err))
                .addSubcommand(new CrudCommand(generateCrud, messages, out, err))
                .addSubcommand(new ClaudeHookCommand(analyzeProject, messages, stdin, err));
        commandLine.setResourceBundle(messages.bundle());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.setOut(new PrintWriter(out, true));
        commandLine.setErr(new PrintWriter(err, true));
        return commandLine.execute(args);
    }
}
