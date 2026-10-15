package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Snippet;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import dev.mauricioandrade.shisho.domain.port.out.ReportWriter;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.json.Json;
import dev.mauricioandrade.shisho.infrastructure.report.ReportTexts.VerdictText;

import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * SARIF 2.1.0, the format GitHub code scanning reads. File locations are relative to the working
 * directory when the project is inside it (in GitHub Actions, the repository root), and relative
 * to the project otherwise. The rank and the verdict go in the run's {@code properties}.
 */
public final class SarifReportWriter implements ReportWriter {

    static final String INFORMATION_URI = "https://github.com/mauricioandrade/shisho";
    private static final String SCHEMA = "https://json.schemastore.org/sarif-2.1.0.json";
    private static final String SOURCE_ROOT = "%SRCROOT%";

    private final PrintStream out;
    private final ReportTexts texts;
    private final String version;
    private final Path workingDirectory;

    public SarifReportWriter(PrintStream out, Messages messages, List<KataGuide> guides, String version, Path workingDirectory) {
        this.out = out;
        this.texts = new ReportTexts(messages, guides);
        this.version = version;
        this.workingDirectory = workingDirectory.toAbsolutePath().normalize();
    }

    @Override
    public void write(Analysis analysis) {
        Map<String, Object> run = new LinkedHashMap<>();
        run.put("tool", Map.of("driver", driver(analysis)));
        run.put("invocations", List.of(invocation(analysis)));
        run.put("results", analysis.lessons().stream().map(lesson -> result(analysis, lesson)).toList());
        run.put("properties", properties(analysis));

        Map<String, Object> sarif = new LinkedHashMap<>();
        sarif.put("$schema", SCHEMA);
        sarif.put("version", "2.1.0");
        sarif.put("runs", List.of(run));
        out.print(Json.write(sarif));
        out.flush();
    }

    private Map<String, Object> driver(Analysis analysis) {
        Map<String, Lesson> firstByRule = analysis.lessons().stream()
                .collect(Collectors.toMap(Lesson::kataId, lesson -> lesson, (first, _) -> first, LinkedHashMap::new));
        Map<String, Object> driver = new LinkedHashMap<>();
        driver.put("name", "Shishō");
        driver.put("informationUri", INFORMATION_URI);
        driver.put("version", version);
        driver.put("rules", firstByRule.values().stream().map(this::rule).toList());
        return driver;
    }

    private Map<String, Object> rule(Lesson lesson) {
        Map<String, Object> rule = new LinkedHashMap<>();
        rule.put("id", lesson.kataId());
        rule.put("shortDescription", Map.of("text", texts.text("kata." + lesson.kataId() + ".title")));
        rule.put("helpUri", INFORMATION_URI + "#katas");
        texts.guide(lesson).ifPresent(guide -> rule.put("help", help(guide)));
        rule.put("defaultConfiguration", Map.of("level", level(lesson.severity())));
        return rule;
    }

    /** Shown by GitHub code scanning next to each alert. */
    private Map<String, Object> help(KataGuide guide) {
        String why = texts.text("html.why");
        String acceptable = texts.text("html.acceptable");
        String before = texts.text("html.before");
        String after = texts.text("html.after");
        String references = texts.text("html.references");
        Map<String, Object> help = new LinkedHashMap<>();
        help.put("text", String.join("\n\n",
                why + ":\n" + String.join("\n\n", guide.why()),
                acceptable + ":\n" + String.join("\n\n", guide.acceptableWhen()),
                before + ":\n" + guide.before().code(),
                after + ":\n" + guide.after().code(),
                references + ":\n" + String.join("\n", guide.references())));
        help.put("markdown", String.join("\n\n",
                markdown(why, guide.why()),
                markdown(acceptable, guide.acceptableWhen()),
                markdown(before, guide.before()),
                markdown(after, guide.after()),
                "**" + references + "**\n\n" + guide.references().stream().map(url -> "- " + url).collect(Collectors.joining("\n"))));
        return help;
    }

    private static String markdown(String label, List<String> paragraphs) {
        return "**" + label + "**\n\n" + String.join("\n\n", paragraphs);
    }

    private static String markdown(String label, Snippet snippet) {
        String explanation = snippet.explanation().isEmpty() ? "" : snippet.explanation() + "\n\n";
        return "**" + label + "**\n\n" + explanation + "```" + snippet.language() + "\n" + snippet.code() + "\n```";
    }

    private Map<String, Object> invocation(Analysis analysis) {
        Map<String, Object> invocation = new LinkedHashMap<>();
        invocation.put("executionSuccessful", true);
        invocation.put("toolExecutionNotifications", analysis.unparseableFiles().stream()
                .map(file -> notification(analysis, file))
                .toList());
        return invocation;
    }

    private Map<String, Object> notification(Analysis analysis, SourceFile.Unparseable file) {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("level", "warning");
        notification.put("message", Map.of("text", texts.unparseable(file)));
        notification.put("locations", List.of(location(analysis, file.file(), file.line())));
        return notification;
    }

    private Map<String, Object> result(Analysis analysis, Lesson lesson) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ruleId", lesson.kataId());
        result.put("level", level(lesson.severity()));
        result.put("message", Map.of("text", texts.explanation(lesson)));
        result.put("locations", List.of(location(analysis, lesson.file(), lesson.line())));
        result.put("properties", Map.of("severity", lesson.severity().name()));
        return result;
    }

    private Map<String, Object> location(Analysis analysis, Path file, int line) {
        Map<String, Object> artifact = new LinkedHashMap<>(); // never Map.of with two keys: its order changes between runs
        artifact.put("uri", uri(analysis, file));
        artifact.put("uriBaseId", SOURCE_ROOT);
        Map<String, Object> physical = new LinkedHashMap<>();
        physical.put("artifactLocation", artifact);
        if (line > 0) {
            physical.put("region", Map.of("startLine", line));
        }
        return Map.of("physicalLocation", physical);
    }

    private Map<String, Object> properties(Analysis analysis) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("project", analysis.projectName());
        properties.put("javaVersion", analysis.javaVersion().feature());
        properties.put("score", analysis.score());
        properties.put("rank", texts.rank(analysis));
        analysis.architecture().ifPresent(match -> {
            properties.put("architecture", architecture(match));
            properties.put("verdict", verdict(match));
        });
        return properties;
    }

    private static Map<String, Object> architecture(ArchitectureMatch match) {
        Map<String, Object> architecture = new LinkedHashMap<>();
        architecture.put("id", match.architecture().id());
        architecture.put("name", match.architecture().name());
        architecture.put("confidence", match.confidence());
        architecture.put("signals", match.signals());
        return architecture;
    }

    private Map<String, Object> verdict(ArchitectureMatch match) {
        VerdictText text = texts.verdict(match);
        Map<String, Object> verdict = new LinkedHashMap<>();
        verdict.put("kind", switch (match.verdict()) {
            case Verdict.Overengineering _ -> "overengineering";
            case Verdict.Underengineering _ -> "underengineering";
            case Verdict.Adequate _ -> "adequate";
        });
        verdict.put("text", text.headline());
        verdict.put("evidence", text.evidence());
        text.next().ifPresent(next -> verdict.put("next", next));
        return verdict;
    }

    private static String level(Severity severity) {
        return switch (severity) {
            case GRAVE -> "error";
            case MEDIA -> "warning";
            case LEVE, INFO -> "note";
        };
    }

    /** Forward slashes and percent-encoding, whatever the operating system. */
    private String uri(Analysis analysis, Path file) {
        Path absolute = analysis.projectRoot().toAbsolutePath().normalize().resolve(file);
        Path relative = absolute.startsWith(workingDirectory) ? workingDirectory.relativize(absolute) : file;
        String path = StreamSupport.stream(relative.spliterator(), false).map(Path::toString).collect(Collectors.joining("/"));
        try {
            return new URI(null, null, path, null).getRawPath();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Not a path: " + path, e);
        }
    }
}
