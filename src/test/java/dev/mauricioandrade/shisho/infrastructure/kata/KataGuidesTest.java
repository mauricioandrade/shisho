package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Example;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** The sensei's guides are code too: the kata must flag "Antes" and accept "Depois". */
class KataGuidesTest {

    private static final String QUARKUS = "quarkus";
    private static final String PROPERTIES = "properties";
    private static final String SPRING_JPA = "org.springframework.boot:spring-boot-starter-data-jpa";
    private static final String QUARKUS_JPA = "io.quarkus:quarkus-hibernate-orm";
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

    @TestFactory
    Stream<DynamicTest> inQuarkusTheKataFlagsTheQuarkusCodeBeforeAndAcceptsTheCodeAfter() {
        return Stream.of(guides, englishGuides).flatMap(language -> KATAS.stream()
                .filter(kata -> language.get(kata.id()).variants().containsKey(QUARKUS))
                .filter(kata -> language.get(kata.id()).variants().get(QUARKUS).before().language().equals("java"))
                .map(kata -> DynamicTest.dynamicTest(kata.id() + (language == guides ? "" : " (en)"), () -> {
                    Example example = language.get(kata.id()).variants().get(QUARKUS);
                    assertThat(quarkusLessons(kata, example.before().code())).as("Antes (Quarkus)").isNotEmpty()
                            .allSatisfy(lesson -> assertThat(lesson.messageKey()).isEqualTo("kata." + kata.id() + ".lesson.quarkus"));
                    assertThat(quarkusLessons(kata, example.after().code())).as("Depois (Quarkus)").isEmpty();
                })));
    }

    /** Settings examples run against a JPA application, the kind every settings kata is about. */
    @TestFactory
    Stream<DynamicTest> theKataFlagsTheSettingsBeforeAndAcceptsTheSettingsAfter() {
        return Stream.of(guides, englishGuides).flatMap(language -> KATAS.stream()
                .filter(kata -> language.get(kata.id()).before().language().equals(PROPERTIES))
                .map(kata -> DynamicTest.dynamicTest(kata.id() + (language == guides ? "" : " (en)"), () -> {
                    KataGuide guide = language.get(kata.id());
                    ProjectKata settingsKata = (ProjectKata) kata;
                    assertThat(KataTestSupport.evaluateSettings(settingsKata, guide.before().code(), SPRING_JPA)).as("Antes").isNotEmpty()
                            .allSatisfy(lesson -> assertThat(lesson.kataId()).isEqualTo(kata.id()));
                    assertThat(KataTestSupport.evaluateSettings(settingsKata, guide.after().code(), SPRING_JPA)).as("Depois").isEmpty();
                })));
    }

    @TestFactory
    Stream<DynamicTest> inQuarkusTheKataFlagsTheSettingsBeforeAndAcceptsTheSettingsAfter() {
        return Stream.of(guides, englishGuides).flatMap(language -> KATAS.stream()
                .filter(kata -> language.get(kata.id()).variants().containsKey(QUARKUS))
                .filter(kata -> language.get(kata.id()).variants().get(QUARKUS).before().language().equals(PROPERTIES))
                .map(kata -> DynamicTest.dynamicTest(kata.id() + (language == guides ? "" : " (en)"), () -> {
                    Example example = language.get(kata.id()).variants().get(QUARKUS);
                    ProjectKata settingsKata = (ProjectKata) kata;
                    assertThat(KataTestSupport.evaluateSettings(settingsKata, example.before().code(), QUARKUS_JPA)).as("Antes (Quarkus)")
                            .isNotEmpty()
                            .allSatisfy(lesson -> assertThat(lesson.messageKey()).isEqualTo("kata." + kata.id() + ".lesson.quarkus"));
                    assertThat(KataTestSupport.evaluateSettings(settingsKata, example.after().code(), QUARKUS_JPA)).as("Depois (Quarkus)")
                            .isEmpty();
                })));
    }

    @Test
    void everyKataThatExplainsTheQuarkusWayShowsItInQuarkus() throws IOException {
        Properties messages = new Properties();
        try (Reader reader = Files.newBufferedReader(Path.of("src/main/resources/messages.properties"))) {
            messages.load(reader);
        }
        List<String> quarkusLessons = messages.stringPropertyNames().stream()
                .filter(key -> key.endsWith(".lesson.quarkus"))
                .map(key -> key.substring("kata.".length(), key.length() - ".lesson.quarkus".length()))
                .toList();

        assertThat(quarkusLessons).isNotEmpty().allSatisfy(id -> {
            assertThat(guides.get(id).variants()).as(id).containsOnlyKeys(QUARKUS);
            assertThat(englishGuides.get(id).variants()).as(id).containsOnlyKeys(QUARKUS);
        });
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
        assertThat(List.of("missing-actuator", "missing-springdoc")).allSatisfy(id ->
                assertThat(guides.get(id).variants().get(QUARKUS).after().code()).contains("<groupId>io.quarkus</groupId>"));
    }

    private static List<Lesson> quarkusLessons(Kata kata, String code) {
        return switch (kata) {
            case ClassKata classKata -> KataTestSupport.evaluate(classKata, code);
            case ProjectKata projectKata -> KataTestSupport.evaluate(projectKata, code, "io.quarkus:quarkus-arc", "io.quarkus:quarkus-rest");
        };
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
