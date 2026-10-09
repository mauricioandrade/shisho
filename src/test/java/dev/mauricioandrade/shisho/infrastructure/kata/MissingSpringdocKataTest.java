package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Severity;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluateBuild;
import static org.assertj.core.api.Assertions.assertThat;

class MissingSpringdocKataTest {

    private final MissingSpringdocKata kata = new MissingSpringdocKata();

    @Test
    void suggestsSpringdocToWebApplications() {
        assertThat(evaluateBuild(kata, "org.springframework.boot:spring-boot-starter-webflux")).singleElement().satisfies(lesson -> {
            assertThat(lesson.file()).isEqualTo(Path.of("pom.xml"));
            assertThat(lesson.line()).isZero();
            assertThat(lesson.severity()).isEqualTo(Severity.INFO);
            assertThat(lesson.messageKey()).isEqualTo("kata.missing-springdoc.lesson");
        });
    }

    @Test
    void acceptsAnySpringdocArtifact() {
        assertThat(evaluateBuild(kata,
                "org.springframework.boot:spring-boot-starter-web",
                "org.springdoc:springdoc-openapi-starter-webmvc-ui")).isEmpty();
    }

    @Test
    void ignoresProjectsThatAreNotWebApplicationsOrHaveNoBuild() {
        assertThat(evaluateBuild(kata, "org.springframework.boot:spring-boot-starter")).isEmpty();
        assertThat(evaluate(kata, "class App {}")).isEmpty();
    }
}
