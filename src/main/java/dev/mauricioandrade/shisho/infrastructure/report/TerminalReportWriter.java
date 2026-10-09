package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.KataExample;
import dev.mauricioandrade.shisho.domain.model.KataExample.Snippet;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.port.out.ReportWriter;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.report.ReportTexts.VerdictText;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The report for people at a terminal: aligned columns, wrapped at {@value #WIDTH} characters.
 * Given the catalog's examples (the sensei mode), it also shows how to fix each kata found.
 */
public final class TerminalReportWriter implements ReportWriter {

    private static final int WIDTH = 100;
    private static final String EVIDENCE_BULLET = "  · ";
    private static final String EVIDENCE_INDENT = "    ";
    private static final String EXAMPLE_LABEL_INDENT = "  ";
    private static final String EXAMPLE_TEXT_INDENT = "    ";
    private static final String EXAMPLE_CODE_INDENT = "      ";

    private final PrintStream out;
    private final ReportTexts texts;

    /** @param examples the catalog's examples for the sensei mode; empty for the plain report */
    public TerminalReportWriter(PrintStream out, Messages messages, List<KataExample> examples) {
        this.out = out;
        this.texts = new ReportTexts(messages, examples);
    }

    @Override
    public void write(Analysis analysis) {
        out.println(texts.header(analysis));
        out.println(texts.architecture(analysis));
        out.println();

        int severityWidth = Arrays.stream(Severity.values()).mapToInt(severity -> texts.severity(severity).length()).max().orElse(0);
        int kataWidth = analysis.lessons().stream().mapToInt(lesson -> lesson.kataId().length() + 2).max().orElse(0);
        String indent = " ".repeat(severityWidth + 4); // explanation starts under the kata column
        for (Lesson lesson : analysis.lessons()) {
            out.println(symbol(lesson.severity()) + " "
                    + pad(texts.severity(lesson.severity()), severityWidth) + "  "
                    + pad("[" + lesson.kataId() + "]", kataWidth) + "  "
                    + location(lesson));
            wrap(texts.explanation(lesson), WIDTH - indent.length()).forEach(line -> out.println(indent + line));
        }
        for (SourceFile.Unparseable file : analysis.unparseableFiles()) {
            out.println(texts.unparseable(file));
        }
        if (!analysis.lessons().isEmpty() || !analysis.unparseableFiles().isEmpty()) {
            out.println();
        }
        List<KataExample> examples = texts.examples(analysis);
        if (!examples.isEmpty()) {
            out.println(texts.text("report.sensei.title"));
            out.println();
            examples.forEach(example -> {
                example(example).forEach(out::println);
                out.println();
            });
        }
        analysis.architecture().ifPresent(match -> {
            verdict(texts.verdict(match)).forEach(out::println);
            out.println();
        });
        out.println(texts.summary(analysis));
    }

    private static List<String> verdict(VerdictText verdict) {
        List<String> lines = new ArrayList<>(wrap(verdict.headline(), WIDTH));
        for (String evidence : verdict.evidence()) {
            List<String> wrapped = wrap(evidence, WIDTH - EVIDENCE_INDENT.length());
            lines.add(EVIDENCE_BULLET + wrapped.getFirst());
            wrapped.stream().skip(1).forEach(line -> lines.add(EVIDENCE_INDENT + line));
        }
        verdict.next().ifPresent(lines::add);
        return lines;
    }

    private List<String> example(KataExample example) {
        List<String> lines = new ArrayList<>();
        lines.add("[" + example.kataId() + "] " + texts.title(example.kataId()));
        snippet(texts.text("report.sensei.before"), example.before(), lines);
        snippet(texts.text("report.sensei.after"), example.after(), lines);
        return lines;
    }

    private static void snippet(String label, Snippet snippet, List<String> lines) {
        lines.add(EXAMPLE_LABEL_INDENT + label);
        if (!snippet.explanation().isEmpty()) {
            wrap(snippet.explanation(), WIDTH - EXAMPLE_TEXT_INDENT.length())
                    .forEach(line -> lines.add(EXAMPLE_TEXT_INDENT + line));
        }
        snippet.code().lines().forEach(line -> lines.add(line.isEmpty() ? "" : EXAMPLE_CODE_INDENT + line));
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
