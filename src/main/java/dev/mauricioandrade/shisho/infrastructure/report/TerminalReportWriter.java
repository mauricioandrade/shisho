package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Concept;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Snippet;
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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The report for people at a terminal: aligned columns, wrapped at {@value #WIDTH} characters.
 * Given the catalog's guides (the sensei mode), it also explains each kata found and how to fix it.
 */
public final class TerminalReportWriter implements ReportWriter {

    public static final int WIDTH = 100;
    private static final String SHOSHIN_INDENT = "  ";
    private static final int SHOSHIN_PLACES = 3;
    private static final String EVIDENCE_BULLET = "  · ";
    private static final String EVIDENCE_INDENT = "    ";
    private static final String EXAMPLE_LABEL_INDENT = "  ";
    private static final String EXAMPLE_TEXT_INDENT = "    ";
    private static final String EXAMPLE_CODE_INDENT = "      ";

    private final PrintStream out;
    private final ReportTexts texts;
    private final List<Concept> concepts;

    /** @param guides the catalog's guides for the sensei mode; empty for the plain report */
    public TerminalReportWriter(PrintStream out, Messages messages, List<KataGuide> guides) {
        this(out, messages, guides, List.of());
    }

    /** @param concepts the catalog's concepts for the shoshin mode; empty for the plain report */
    public TerminalReportWriter(PrintStream out, Messages messages, List<KataGuide> guides, List<Concept> concepts) {
        this.out = out;
        this.texts = new ReportTexts(messages, guides);
        this.concepts = List.copyOf(concepts);
    }

    @Override
    public void write(Analysis analysis) {
        out.println(texts.header(analysis));
        out.println(texts.architecture(analysis));
        out.println();

        writeLessons(analysis.lessons());
        for (SourceFile.Unparseable file : analysis.unparseableFiles()) {
            out.println(texts.unparseable(file));
        }
        if (!analysis.lessons().isEmpty() || !analysis.unparseableFiles().isEmpty()) {
            out.println();
        }
        List<KataGuide> guides = texts.guides(analysis);
        if (!guides.isEmpty()) {
            out.println(texts.text("report.sensei.title"));
            out.println();
            guides.forEach(guide -> {
                guide(guide).forEach(out::println);
                out.println();
            });
        }
        if (!concepts.isEmpty()) {
            shoshin(analysis.concepts());
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
        verdict.next().ifPresent(next -> lines.addAll(wrap(next, WIDTH)));
        return lines;
    }

    private List<String> guide(KataGuide guide) {
        List<String> lines = new ArrayList<>();
        lines.add("[" + guide.kataId() + "] " + texts.title(guide.kataId()));
        prose(texts.text("report.sensei.why"), guide.why(), lines);
        prose(texts.text("report.sensei.acceptable"), guide.acceptableWhen(), lines);
        snippet(texts.text("report.sensei.before"), guide.before(), lines);
        snippet(texts.text("report.sensei.after"), guide.after(), lines);
        lines.add(EXAMPLE_LABEL_INDENT + texts.text("report.sensei.references"));
        guide.references().forEach(url -> lines.add(EXAMPLE_TEXT_INDENT + url));
        return lines;
    }

    /** The paragraphs under the label, with a blank line between them. */
    private static void prose(String label, List<String> paragraphs, List<String> lines) {
        lines.add(EXAMPLE_LABEL_INDENT + label);
        for (int i = 0; i < paragraphs.size(); i++) {
            if (i > 0) {
                lines.add("");
            }
            wrap(paragraphs.get(i), WIDTH - EXAMPLE_TEXT_INDENT.length()).forEach(line -> lines.add(EXAMPLE_TEXT_INDENT + line));
        }
    }

    private static void snippet(String label, Snippet snippet, List<String> lines) {
        lines.add(EXAMPLE_LABEL_INDENT + label);
        if (!snippet.explanation().isEmpty()) {
            wrap(snippet.explanation(), WIDTH - EXAMPLE_TEXT_INDENT.length())
                    .forEach(line -> lines.add(EXAMPLE_TEXT_INDENT + line));
        }
        snippet.code().lines().forEach(line -> lines.add(line.isEmpty() ? "" : EXAMPLE_CODE_INDENT + line));
    }

    /** The concepts found, by group, each with where it appears, what it is and where to read more. See docs/adr/0009-shoshin.md. */
    private void shoshin(List<ConceptFinding> findings) {
        out.println(texts.text("report.shoshin.title"));
        out.println();
        if (findings.isEmpty()) {
            out.println(texts.text("report.shoshin.none"));
            out.println();
            return;
        }
        Map<String, Concept> byId = concepts.stream().collect(Collectors.toMap(Concept::id, Function.identity()));
        for (Concept.Group group : Concept.Group.values()) {
            List<ConceptFinding> inGroup = findings.stream()
                    .filter(finding -> byId.containsKey(finding.conceptId()) && byId.get(finding.conceptId()).group() == group)
                    .toList();
            if (inGroup.isEmpty()) {
                continue;
            }
            out.println(texts.text("report.shoshin.group." + group.name()));
            for (ConceptFinding finding : inGroup) {
                Concept concept = byId.get(finding.conceptId());
                out.println(SHOSHIN_INDENT + concept.name() + "  " + where(finding.occurrences()));
                String indent = SHOSHIN_INDENT.repeat(2);
                concept.explanation().forEach(paragraph -> {
                    wrap(paragraph, WIDTH - indent.length()).forEach(line -> out.println(indent + line));
                    out.println();
                });
                out.println(indent + texts.text("report.shoshin.more-info", concept.references().getFirst()));
                out.println();
            }
        }
    }

    private String where(List<ConceptFinding.Occurrence> occurrences) {
        String shown = occurrences.stream().limit(SHOSHIN_PLACES)
                .map(occurrence -> occurrence.file().getFileName() + ":" + occurrence.line())
                .collect(Collectors.joining(", "));
        int rest = occurrences.size() - SHOSHIN_PLACES;
        return texts.text("report.shoshin.where", occurrences.size(), rest > 0 ? shown + " " + texts.text("report.shoshin.and-more", rest) : shown);
    }

    /** One line per lesson (severity, kata, file and line), with its explanation below. */
    public void writeLessons(List<Lesson> lessons) {
        int severityWidth = Arrays.stream(Severity.values()).mapToInt(severity -> texts.severity(severity).length()).max().orElse(0);
        int kataWidth = lessons.stream().mapToInt(lesson -> lesson.kataId().length() + 2).max().orElse(0);
        String indent = " ".repeat(severityWidth + 4); // explanation starts under the kata column
        for (Lesson lesson : lessons) {
            out.println(symbol(lesson.severity()) + " "
                    + pad(texts.severity(lesson.severity()), severityWidth) + "  "
                    + pad("[" + lesson.kataId() + "]", kataWidth) + "  "
                    + ReportTexts.location(lesson));
            wrap(texts.explanation(lesson), WIDTH - indent.length()).forEach(line -> out.println(indent + line));
        }
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

    /** Splits {@code text} into lines of at most {@code width} characters, breaking only between words. */
    public static List<String> wrap(String text, int width) {
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
