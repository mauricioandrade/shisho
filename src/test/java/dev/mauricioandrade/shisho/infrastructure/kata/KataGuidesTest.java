package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** The sensei's guides are code too: the kata must flag "Antes" and accept "Depois". */
class KataGuidesTest {

    private static final List<Kata> KATAS = Katas.all(MarkdownCatalogRepository.bundled().avoidedDependencies());

    private final Map<String, KataGuide> guides = guides(Optional.empty());
    private final Map<String, KataGuide> englishGuides = guides(Optional.of("en"));

    @Test
    void everyKataAndTheDependencyRuleHaveAnExample() {
        assertThat(guides.keySet()).containsExactlyInAnyOrderElementsOf(
                Stream.concat(KATAS.stream().map(Kata::id), Stream.of(ArchitectureMatch.DEPENDENCY_RULE)).toList());
    }

    @TestFactory
    Stream<DynamicTest> theKataFlagsTheCodeBeforeAndAcceptsTheCodeAfter() {
        return Stream.of(guides, englishGuides).flatMap(language -> KATAS.stream()
                .filter(kata -> language.get(kata.id()).before().language().equals("java"))
                .map(kata -> DynamicTest.dynamicTest(kata.id() + (language == guides ? "" : " (en)"), () -> {
                    KataGuide guide = language.get(kata.id());
                    assertThat(lessons(kata, guide.before().code())).as("Antes").isNotEmpty()
                            .allSatisfy(lesson -> assertThat(lesson.kataId()).isEqualTo(kata.id()));
                    assertThat(lessons(kata, guide.after().code())).as("Depois").isEmpty();
                })));
    }

    @Test
    void theEnglishGuidesTranslateTheTextAndKeepTheSources() {
        assertThat(englishGuides.keySet()).isEqualTo(guides.keySet());
        assertThat(englishGuides.values()).allSatisfy(guide -> {
            assertThat(guide.why()).as(guide.kataId()).isNotEqualTo(guides.get(guide.kataId()).why());
            assertThat(guide.references()).as(guide.kataId()).isEqualTo(guides.get(guide.kataId()).references());
        });
    }

    private static Map<String, KataGuide> guides(Optional<String> translation) {
        return MarkdownCatalogRepository.bundled(translation).kataGuides().stream()
                .collect(Collectors.toMap(KataGuide::kataId, Function.identity()));
    }

    @Test
    void everyGuideCitesItsSourcesByUrl() {
        assertThat(guides.values()).allSatisfy(guide ->
                assertThat(guide.references()).as(guide.kataId()).isNotEmpty().allMatch(url -> url.startsWith("https://")));
    }

    @Test
    void buildExamplesAreXml() {
        assertThat(List.of("missing-actuator", "missing-springdoc", "avoided-dependency")).allSatisfy(id ->
                assertThat(guides.get(id).after().code()).startsWith("<dependencies>"));
    }

    private static List<Lesson> lessons(Kata kata, String code) {
        return switch (kata) {
            case ClassKata classKata -> KataTestSupport.evaluate(classKata, code);
            // the Quarkus-only katas need a Quarkus build to say anything
            case ProjectKata projectKata -> kata.id().startsWith("quarkus-")
                    ? KataTestSupport.evaluate(projectKata, code, "io.quarkus:quarkus-arc")
                    : KataTestSupport.evaluate(projectKata, code);
        };
    }
}
