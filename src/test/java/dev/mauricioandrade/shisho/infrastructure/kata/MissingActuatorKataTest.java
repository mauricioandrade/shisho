package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluateBuild;
import static org.assertj.core.api.Assertions.assertThat;

class MissingActuatorKataTest {

    private final MissingActuatorKata kata = new MissingActuatorKata();

    @Test
    void flagsWebApplicationsWithoutActuator() {
        assertThat(evaluateBuild(kata, "org.springframework.boot:spring-boot-starter-web")).singleElement().satisfies(lesson -> {
            assertThat(lesson.file()).isEqualTo(Path.of("pom.xml"));
            assertThat(lesson.line()).isZero();
            assertThat(lesson.messageKey()).isEqualTo("kata.missing-actuator.lesson");
        });
        assertThat(evaluateBuild(kata, "org.springframework.boot:spring-boot-starter-webmvc")).hasSize(1);
        assertThat(evaluateBuild(kata, "org.springframework.boot:spring-boot-starter-webflux")).hasSize(1);
    }

    @Test
    void acceptsWebApplicationsWithActuator() {
        assertThat(evaluateBuild(kata,
                "org.springframework.boot:spring-boot-starter-web",
                "org.springframework.boot:spring-boot-starter-actuator")).isEmpty();
    }

    @Test
    void flagsQuarkusRestServersWithoutSmallRyeHealth() {
        assertThat(evaluateBuild(kata, "io.quarkus:quarkus-rest-jackson")).singleElement().satisfies(lesson ->
                assertThat(lesson.messageKey()).isEqualTo("kata.missing-actuator.lesson.quarkus"));
        assertThat(evaluateBuild(kata, "io.quarkus:quarkus-resteasy")).hasSize(1);
        assertThat(evaluateBuild(kata, "io.quarkus:quarkus-rest", "io.quarkus:quarkus-smallrye-health")).isEmpty();
    }

    @Test
    void ignoresProjectsThatAreNotWebApplicationsOrHaveNoBuild() {
        assertThat(evaluateBuild(kata, "org.springframework.boot:spring-boot-starter-batch")).isEmpty();
        assertThat(evaluateBuild(kata, "io.quarkus:quarkus-arc", "io.quarkus:quarkus-rest-client-jackson")).isEmpty();
        assertThat(evaluate(kata, "class App {}")).isEmpty();
    }
}
