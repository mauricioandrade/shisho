package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import dev.mauricioandrade.shisho.domain.port.out.ReportWriter;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** The only place in the Shishō that writes to the console. */
public final class TerminalReportWriter implements ReportWriter {

    private static final int WIDTH = 100;
    private static final String EVIDENCE_BULLET = "  · ";
    private static final String EVIDENCE_INDENT = "    ";

    private final PrintStream out;
    private final Messages messages;

    public TerminalReportWriter(PrintStream out, Messages messages) {
        this.out = out;
        this.messages = messages;
    }

    @Override
    public void write(Analysis analysis) {
        String header = analysis.javaVersionDeclared() ? "report.header" : "report.header.assumed";
        out.println(text(header, analysis.projectName(), analysis.javaVersion().toString()));
        out.println(analysis.architecture()
                .map(match -> text("report.architecture",
                        match.architecture().name(), match.confidence(), String.join(", ", match.signals())))
                .orElseGet(() -> text("report.architecture.none")));
        out.println();

        int severityWidth = Arrays.stream(Severity.values()).mapToInt(severity -> severityLabel(severity).length()).max().orElse(0);
        int kataWidth = analysis.lessons().stream().mapToInt(lesson -> lesson.kataId().length() + 2).max().orElse(0);
        String indent = " ".repeat(severityWidth + 4); // explanation starts under the kata column
        for (Lesson lesson : analysis.lessons()) {
            out.println(symbol(lesson.severity()) + " "
                    + pad(severityLabel(lesson.severity()), severityWidth) + "  "
                    + pad("[" + lesson.kataId() + "]", kataWidth) + "  "
                    + location(lesson));
            wrap(text(lesson.messageKey(), lesson.arguments().toArray()), WIDTH - indent.length())
                    .forEach(line -> out.println(indent + line));
        }
        for (SourceFile.Unparseable file : analysis.unparseableFiles()) {
            out.println(text("report.unparseable", file.file().toString(), String.valueOf(file.line()), file.problem()));
        }
        if (!analysis.lessons().isEmpty() || !analysis.unparseableFiles().isEmpty()) {
            out.println();
        }
        analysis.architecture().ifPresent(match -> {
            verdict(match).forEach(out::println);
            out.println();
        });
        out.println(text("report.summary", text("rank." + analysis.rank().name()), summary(analysis)));
    }

    private List<String> verdict(ArchitectureMatch match) {
        ArchitectureProfile architecture = match.architecture();
        List<String> lines = new ArrayList<>();
        List<Evidence> evidence = switch (match.verdict()) {
            case Verdict.Overengineering over -> {
                lines.addAll(wrap(over.unjustified()
                        ? text("verdict.overengineering.unjustified", architecture.name(),
                                architecture.cost().classesPerFeature().min(), architecture.cost().classesPerFeature().max(),
                                String.join(", ", architecture.makesSenseWhen()))
                        : text("verdict.overengineering", architecture.name()), WIDTH));
                yield over.evidence();
            }
            case Verdict.Underengineering under -> {
                lines.addAll(wrap(text("verdict.underengineering", architecture.name()), WIDTH));
                yield under.evidence();
            }
            case Verdict.Adequate adequate -> {
                String key = !adequate.contextDeclared() ? "verdict.adequate.no-context"
                        : adequate.justifications().isEmpty() ? "verdict.adequate"
                        : "verdict.adequate.justified";
                lines.addAll(wrap(text(key, architecture.name(), String.join(", ", adequate.justifications())), WIDTH));
                yield List.of();
            }
        };
        for (Evidence item : evidence) {
            List<String> wrapped = wrap(text(item.messageKey(), item.arguments().toArray()), WIDTH - EVIDENCE_INDENT.length());
            lines.add(EVIDENCE_BULLET + wrapped.getFirst());
            wrapped.stream().skip(1).forEach(line -> lines.add(EVIDENCE_INDENT + line));
        }
        switch (match.verdict()) {
            case Verdict.Overengineering over -> over.simplerAlternative()
                    .ifPresent(simpler -> lines.add(text("verdict.simpler", simpler)));
            case Verdict.Underengineering under when !under.evolvesTo().isEmpty() ->
                    lines.add(text("verdict.evolves-to", String.join(", ", under.evolvesTo())));
            default -> {
            }
        }
        return lines;
    }

    private String summary(Analysis analysis) {
        if (analysis.lessons().isEmpty()) {
            return text("report.summary.none");
        }
        return text("report.summary.counts",
                text("report.count.lessons", analysis.lessons().size()),
                text("report.count.grave", analysis.count(Severity.GRAVE)),
                text("report.count.media", analysis.count(Severity.MEDIA)),
                text("report.count.leve", analysis.count(Severity.LEVE)),
                text("report.count.info", analysis.count(Severity.INFO)));
    }

    private static String location(Lesson lesson) {
        String file = lesson.file().getFileName().toString();
        return lesson.line() > 0 ? file + ":" + lesson.line() : file;
    }

    private static String symbol(Severity severity) {
        return switch (severity) {
            case GRAVE -> "✗";
            case MEDIA -> "!";
            case LEVE -> "·";
            case INFO -> "i";
        };
    }

    private String severityLabel(Severity severity) {
        return text("severity." + severity.name());
    }

    private String text(String key, Object... arguments) {
        return messages.text(key, arguments);
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(Math.max(0, width - text.length()));
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (!line.isEmpty() && line.length() + 1 + word.length() > width) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (!line.isEmpty()) {
                line.append(' ');
            }
            line.append(word);
        }
        lines.add(line.toString());
        return lines;
    }
}
