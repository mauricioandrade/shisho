package dev.mauricioandrade.shisho.infrastructure.report;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The sentences every report format, and the prompt of the local LLM, share in the user's language. */
public final class ReportTexts {

    private final Messages messages;
    private final Map<String, KataGuide> guides;

    public ReportTexts(Messages messages, List<KataGuide> guides) {
        this.messages = messages;
        this.guides = guides.stream().collect(Collectors.toMap(KataGuide::kataId, Function.identity()));
    }

    /** The guides of the katas that taught a lesson, in the order of their first lesson. */
    List<KataGuide> guides(Analysis analysis) {
        return analysis.lessons().stream()
                .map(Lesson::kataId)
                .distinct()
                .flatMap(kataId -> Optional.ofNullable(guides.get(kataId)).stream())
                .toList();
    }

    Optional<KataGuide> guide(String kataId) {
        return Optional.ofNullable(guides.get(kataId));
    }

    public String title(String kataId) {
        return text("kata." + kataId + ".title");
    }

    /** The verdict as a headline, the evidence behind it and where to go next. */
    public record VerdictText(String headline, List<String> evidence, Optional<String> next) {
    }

    public String header(Analysis analysis) {
        String key = analysis.javaVersionDeclared() ? "report.header" : "report.header.assumed";
        return text(key, analysis.projectName(), analysis.javaVersion().toString());
    }

    public String architecture(Analysis analysis) {
        return analysis.architecture()
                .map(match -> text("report.architecture",
                        match.architecture().name(), match.confidence(), String.join(", ", match.signals())))
                .orElseGet(() -> text("report.architecture.none"));
    }

    public String explanation(Lesson lesson) {
        String explanation = text(lesson.messageKey(), lesson.arguments().toArray());
        return lesson.afterUpgradeTo()
                .map(version -> text("report.after-upgrade", version.toString(), explanation))
                .orElse(explanation);
    }

    /** {@code UsuarioController.java:14}, or only the file name for a lesson about the whole file. */
    public static String location(Lesson lesson) {
        String file = lesson.file().getFileName().toString();
        return lesson.line() > 0 ? file + ":" + lesson.line() : file;
    }

    public String severity(Severity severity) {
        return text("severity." + severity.name());
    }

    String unparseable(SourceFile.Unparseable file) {
        return text("report.unparseable", file.file().toString(), String.valueOf(file.line()), file.problem());
    }

    public VerdictText verdict(ArchitectureMatch match) {
        ArchitectureProfile architecture = match.architecture();
        return switch (match.verdict()) {
            case Verdict.Overengineering over -> new VerdictText(
                    over.unjustified()
                            ? text("verdict.overengineering.unjustified", architecture.name(),
                                    architecture.cost().classesPerFeature().min(), architecture.cost().classesPerFeature().max(),
                                    String.join(", ", architecture.makesSenseWhen()))
                            : text("verdict.overengineering", architecture.name()),
                    evidence(over.evidence()),
                    over.simplerAlternative().map(simpler -> text("verdict.simpler", simpler)));
            case Verdict.Underengineering under -> new VerdictText(
                    text("verdict.underengineering", architecture.name()),
                    evidence(under.evidence()),
                    under.evolvesTo().isEmpty()
                            ? Optional.empty()
                            : Optional.of(text("verdict.evolves-to", String.join(", ", under.evolvesTo()))));
            case Verdict.Adequate adequate -> {
                String key = !adequate.contextDeclared() ? "verdict.adequate.no-context"
                        : adequate.justifications().isEmpty() ? "verdict.adequate"
                        : "verdict.adequate.justified";
                yield new VerdictText(
                        text(key, architecture.name(), String.join(", ", adequate.justifications())), List.of(), Optional.empty());
            }
        };
    }

    public String summary(Analysis analysis) {
        return text("report.summary", rank(analysis), counts(analysis));
    }

    /** "12 lições: 2 graves, 6 médias, 4 leves", or "nenhuma lição". */
    String counts(Analysis analysis) {
        return analysis.lessons().isEmpty()
                ? text("report.summary.none")
                : text("report.summary.counts",
                        text("report.count.lessons", analysis.lessons().size()),
                        text("report.count.grave", analysis.count(Severity.GRAVE)),
                        text("report.count.media", analysis.count(Severity.MEDIA)),
                        text("report.count.leve", analysis.count(Severity.LEVE)),
                        text("report.count.info", analysis.count(Severity.INFO)));
    }

    String rank(Analysis analysis) {
        return text("rank." + analysis.rank().name());
    }

    public String text(String key, Object... arguments) {
        return messages.text(key, arguments);
    }

    private List<String> evidence(List<Evidence> evidence) {
        return evidence.stream().map(item -> text(item.messageKey(), item.arguments().toArray())).toList();
    }
}
