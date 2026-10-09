package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.AvoidedDependency;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluateBuild;
import static org.assertj.core.api.Assertions.assertThat;

class AvoidedDependencyKataTest {

    private static final AvoidedDependency SPRINGFOX = new AvoidedDependency("springfox", "Springfox",
            List.of(new Dependency("io.springfox", "springfox-swagger2"), new Dependency("io.springfox", "springfox-swagger-ui")),
            "sem versão nova desde 2020", "springdoc-openapi", List.of("https://springdoc.org/"), "## Por quê\n\nX.");

    private final AvoidedDependencyKata kata = new AvoidedDependencyKata(List.of(SPRINGFOX));

    @Test
    void flagsEachDeclaredCoordinateWithTheReasonAndTheAlternative() {
        assertThat(evaluateBuild(kata, "io.springfox:springfox-swagger2", "io.springfox:springfox-swagger-ui"))
                .hasSize(2)
                .first().satisfies(lesson -> {
                    assertThat(lesson.file()).isEqualTo(Path.of("pom.xml"));
                    assertThat(lesson.messageKey()).isEqualTo("kata.avoided-dependency.lesson");
                    assertThat(lesson.arguments()).containsExactly(
                            "Springfox", "io.springfox:springfox-swagger2", "sem versão nova desde 2020", "springdoc-openapi");
                });
    }

    @Test
    void acceptsBuildsWithoutThemAndProjectsWithoutABuild() {
        assertThat(evaluateBuild(kata, "org.springdoc:springdoc-openapi-starter-webmvc-ui", "io.springfox:springfox-core")).isEmpty();
        assertThat(evaluate(kata, "class App {}")).isEmpty();
    }

    @Test
    void theRealCatalogFlagsLog4jOne() {
        AvoidedDependencyKata real = new AvoidedDependencyKata(
                MarkdownCatalogRepository.bundled().avoidedDependencies());

        assertThat(evaluateBuild(real, "log4j:log4j", "org.apache.logging.log4j:log4j-core"))
                .singleElement().satisfies(lesson -> assertThat(lesson.arguments()).startsWith("Log4j 1.x", "log4j:log4j"));
    }
}
