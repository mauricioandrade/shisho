package dev.mauricioandrade.shisho.infrastructure.llm;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import dev.mauricioandrade.shisho.infrastructure.report.ReportTexts;
import dev.mauricioandrade.shisho.infrastructure.report.ReportTexts.VerdictText;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * What the local LLM reads: the analysis in the same words the terminal report uses, and the
 * catalog Markdown of the detected architecture and of those the verdict points to. The model
 * gets facts to explain, never something to decide. See docs/adr/0004-local-llm.md.
 */
public final class SenseiPrompt {

    /** Lessons per kata shown to the model; a large project would otherwise flood its context. */
    static final int EXAMPLES_PER_KATA = 3;

    private final ReportTexts texts;
    private final Map<String, ArchitectureProfile> architectures;

    public SenseiPrompt(Messages messages, List<ArchitectureProfile> architectures) {
        this.texts = new ReportTexts(messages, List.of());
        this.architectures = architectures.stream().collect(Collectors.toMap(ArchitectureProfile::id, Function.identity()));
    }

    /** The sensei's role and the rules that keep the model to the facts. */
    public String instructions() {
        return texts.text("ask.prompt.instructions");
    }

    /** The facts, then the question; without one, the model explains the verdict. */
    public String facts(Analysis analysis, Optional<String> question) {
        StringBuilder prompt = new StringBuilder()
                .append(texts.header(analysis)).append('\n')
                .append(texts.architecture(analysis)).append('\n');
        analysis.architecture().ifPresent(match -> verdict(match, prompt));
        prompt.append(texts.summary(analysis)).append('\n')
                .append(texts.text("ask.prompt.score", analysis.score(),
                        Severity.GRAVE.penalty(), Severity.MEDIA.penalty(), Severity.LEVE.penalty()))
                .append("\n\n");

        lessons(analysis, prompt);
        architectures(analysis, prompt);

        return prompt.append("## ").append(texts.text("ask.prompt.question")).append("\n\n")
                .append(question.filter(text -> !text.isBlank()).orElseGet(() -> texts.text("ask.prompt.default-question")))
                .append('\n')
                .toString();
    }

    private void verdict(ArchitectureMatch match, StringBuilder prompt) {
        VerdictText verdict = texts.verdict(match);
        prompt.append(verdict.headline()).append('\n');
        verdict.evidence().forEach(evidence -> prompt.append("- ").append(evidence).append('\n'));
        verdict.next().ifPresent(next -> prompt.append(next).append('\n'));
    }

    /** One entry per kata, the most severe first, with how many lessons it taught and the first few of them. */
    private void lessons(Analysis analysis, StringBuilder prompt) {
        prompt.append("## ").append(texts.text("ask.prompt.lessons")).append("\n\n");
        if (analysis.lessons().isEmpty()) {
            prompt.append(texts.text("ask.prompt.no-lessons")).append("\n\n");
            return;
        }
        Map<String, List<Lesson>> byKata = analysis.lessons().stream()
                .collect(Collectors.groupingBy(Lesson::kataId, LinkedHashMap::new, Collectors.toList()));
        byKata.values().stream().sorted(Comparator.comparing(lessons -> lessons.getFirst().severity())).forEach(lessons -> {
            String kataId = lessons.getFirst().kataId();
            prompt.append("- ").append(texts.text("ask.prompt.kata",
                    kataId, texts.title(kataId), texts.severity(lessons.getFirst().severity()), lessons.size())).append('\n');
            lessons.stream().limit(EXAMPLES_PER_KATA).forEach(lesson -> prompt
                    .append("  - ").append(ReportTexts.location(lesson)).append(": ").append(texts.explanation(lesson)).append('\n'));
            if (lessons.size() > EXAMPLES_PER_KATA) {
                prompt.append("  - ").append(texts.text("ask.prompt.more", lessons.size() - EXAMPLES_PER_KATA)).append('\n');
            }
        });
        prompt.append('\n');
    }

    /** The detected architecture, then the simpler or the next one the verdict suggests. */
    private void architectures(Analysis analysis, StringBuilder prompt) {
        List<ArchitectureProfile> relevant = analysis.architecture().stream()
                .flatMap(match -> Stream.concat(Stream.of(match.architecture().id()), suggested(match.verdict())))
                .distinct()
                .flatMap(id -> Optional.ofNullable(architectures.get(id)).stream())
                .toList();
        if (relevant.isEmpty()) {
            return;
        }
        prompt.append("## ").append(texts.text("ask.prompt.architectures")).append("\n\n");
        relevant.forEach(architecture -> prompt
                .append("### ").append(architecture.name()).append("\n\n")
                .append(nested(architecture.markdown().strip())).append("\n\n"));
    }

    /** The catalog's {@code ## O que é} becomes {@code #### O que é}, under the architecture's own heading. */
    private static String nested(String markdown) {
        return markdown.replaceAll("(?m)^#", "###");
    }

    private static Stream<String> suggested(Verdict verdict) {
        return switch (verdict) {
            case Verdict.Overengineering over -> over.simplerAlternative().stream();
            case Verdict.Underengineering under -> under.evolvesTo().stream();
            case Verdict.Adequate adequate -> Stream.empty();
        };
    }
}
