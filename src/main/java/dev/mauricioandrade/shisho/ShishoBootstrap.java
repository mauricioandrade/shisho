package dev.mauricioandrade.shisho;

import dev.mauricioandrade.shisho.application.usecase.AnalyzeProjectService;
import dev.mauricioandrade.shisho.application.usecase.ClassifyArchitectureService;
import dev.mauricioandrade.shisho.application.usecase.FixProjectService;
import dev.mauricioandrade.shisho.application.usecase.RankCalculator;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.domain.port.in.FixProject;
import dev.mauricioandrade.shisho.infrastructure.build.MavenBuildFileReader;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import dev.mauricioandrade.shisho.infrastructure.fix.JavaParserSourceFixer;
import dev.mauricioandrade.shisho.infrastructure.cli.AnalyzeCommand;
import dev.mauricioandrade.shisho.infrastructure.cli.FixCommand;
import dev.mauricioandrade.shisho.infrastructure.cli.ReportWriterFactory;
import dev.mauricioandrade.shisho.infrastructure.cli.ShishoCommand;
import dev.mauricioandrade.shisho.infrastructure.config.YamlProjectConfigReader;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.kata.Katas;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import dev.mauricioandrade.shisho.infrastructure.report.HtmlReportWriter;
import dev.mauricioandrade.shisho.infrastructure.report.SarifReportWriter;
import dev.mauricioandrade.shisho.infrastructure.report.TerminalReportWriter;
import picocli.CommandLine;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** Composition root: builds the object graph by hand and runs the command line. */
public final class ShishoBootstrap {

    private ShishoBootstrap() {
    }

    public static void main(String[] args) {
        System.exit(run(args, new FileOutputStream(FileDescriptor.out), new FileOutputStream(FileDescriptor.err)));
    }

    /** Output is always UTF-8: the report has 師匠, accents and box symbols. */
    static int run(String[] args, OutputStream stdout, OutputStream stderr) {
        PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8);
        Messages messages = new Messages(Messages.DEFAULT_LOCALE);

        MarkdownCatalogRepository catalog = MarkdownCatalogRepository.bundled();
        AnalyzeProject analyzeProject = new AnalyzeProjectService(
                new MavenBuildFileReader(),
                new YamlProjectConfigReader(),
                new JavaParserSourceParser(),
                Katas.all(),
                new ClassifyArchitectureService(catalog, Detectors.all()),
                new RankCalculator());

        ReportWriterFactory reportWriters = (format, destination, sensei) -> switch (format) {
            case TERMINAL -> new TerminalReportWriter(destination, messages, sensei ? catalog.kataExamples() : List.of());
            case SARIF -> new SarifReportWriter(destination, messages, catalog.kataExamples(), ShishoCommand.version(), Path.of(""));
            case HTML -> new HtmlReportWriter(destination, messages, catalog.kataExamples(), ShishoCommand.version());
        };

        FixProject fixProject = new FixProjectService(analyzeProject, new JavaParserSourceFixer());

        CommandLine commandLine = new CommandLine(new ShishoCommand())
                .addSubcommand(new AnalyzeCommand(analyzeProject, reportWriters, messages, out, err))
                .addSubcommand(new FixCommand(fixProject, messages, out, err));
        commandLine.setResourceBundle(messages.bundle());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);
        commandLine.setOut(new PrintWriter(out, true));
        commandLine.setErr(new PrintWriter(err, true));
        return commandLine.execute(args);
    }
}
