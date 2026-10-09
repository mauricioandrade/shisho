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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** The sensei's guides are code too: the kata must flag "Antes" and accept "Depois". */
class KataGuidesTest {

    private final Map<String, KataGuide> guides = MarkdownCatalogRepository.bundled().kataGuides().stream()
            .collect(Collectors.toMap(KataGuide::kataId, Function.identity()));

    @Test
    void everyKataAndTheDependencyRuleHaveAnExample() {
        assertThat(guides.keySet()).containsExactlyInAnyOrderElementsOf(
                Stream.concat(Katas.all().stream().map(Kata::id), Stream.of(ArchitectureMatch.DEPENDENCY_RULE)).toList());
    }

    @TestFactory
    Stream<DynamicTest> theKataFlagsTheCodeBeforeAndAcceptsTheCodeAfter() {
        return Katas.all().stream()
                .filter(kata -> guides.get(kata.id()).before().language().equals("java"))
                .map(kata -> DynamicTest.dynamicTest(kata.id(), () -> {
                    KataGuide guide = guides.get(kata.id());
                    assertThat(lessons(kata, guide.before().code())).as("Antes").isNotEmpty()
                            .allSatisfy(lesson -> assertThat(lesson.kataId()).isEqualTo(kata.id()));
                    assertThat(lessons(kata, guide.after().code())).as("Depois").isEmpty();
                }));
    }

    @Test
    void everyGuideCitesItsSourcesByUrl() {
        assertThat(guides.values()).allSatisfy(guide ->
                assertThat(guide.references()).as(guide.kataId()).isNotEmpty().allMatch(url -> url.startsWith("https://")));
    }

    @Test
    void buildExamplesAreXml() {
        assertThat(List.of("missing-actuator", "missing-springdoc")).allSatisfy(id ->
                assertThat(guides.get(id).after().code()).startsWith("<dependencies>"));
    }

    private static List<Lesson> lessons(Kata kata, String code) {
        return switch (kata) {
            case ClassKata classKata -> KataTestSupport.evaluate(classKata, code);
            case ProjectKata projectKata -> KataTestSupport.evaluate(projectKata, code);
        };
    }
}
