package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.KataExample;
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

/** The sensei's examples are code too: the kata must flag "Antes" and accept "Depois". */
class KataExamplesTest {

    private final Map<String, KataExample> examples = MarkdownCatalogRepository.bundled().kataExamples().stream()
            .collect(Collectors.toMap(KataExample::kataId, Function.identity()));

    @Test
    void everyKataAndTheDependencyRuleHaveAnExample() {
        assertThat(examples.keySet()).containsExactlyInAnyOrderElementsOf(
                Stream.concat(Katas.all().stream().map(Kata::id), Stream.of(ArchitectureMatch.DEPENDENCY_RULE)).toList());
    }

    @TestFactory
    Stream<DynamicTest> theKataFlagsTheCodeBeforeAndAcceptsTheCodeAfter() {
        return Katas.all().stream()
                .filter(kata -> examples.get(kata.id()).before().language().equals("java"))
                .map(kata -> DynamicTest.dynamicTest(kata.id(), () -> {
                    KataExample example = examples.get(kata.id());
                    assertThat(lessons(kata, example.before().code())).as("Antes").isNotEmpty()
                            .allSatisfy(lesson -> assertThat(lesson.kataId()).isEqualTo(kata.id()));
                    assertThat(lessons(kata, example.after().code())).as("Depois").isEmpty();
                }));
    }

    @Test
    void buildExamplesAreXml() {
        assertThat(List.of("missing-actuator", "missing-springdoc")).allSatisfy(id ->
                assertThat(examples.get(id).after().code()).startsWith("<dependencies>"));
    }

    private static List<Lesson> lessons(Kata kata, String code) {
        return switch (kata) {
            case ClassKata classKata -> KataTestSupport.evaluate(classKata, code);
            case ProjectKata projectKata -> KataTestSupport.evaluate(projectKata, code);
        };
    }
}
