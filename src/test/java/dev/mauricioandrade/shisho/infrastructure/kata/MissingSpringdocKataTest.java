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
    void ignoresApplicationsWhoseControllersOnlyRenderViews() {
        String web = "org.springframework.boot:spring-boot-starter-web";
        assertThat(evaluate(kata, """
                @Controller
                class OwnerController {
                    String list(Model model) { return "owners/list"; }
                }
                """, web)).as("no API to document").isEmpty();
        assertThat(evaluate(kata, """
                @Controller
                class OwnerController {
                    String list(Model model) { return "owners/list"; }
                }

                @RestController
                class OwnerApi {}
                """, web)).hasSize(1);
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

    @Test
    void suggestsSmallRyeOpenApiToQuarkusRestServers() {
        assertThat(evaluateBuild(kata, "io.quarkus:quarkus-rest")).singleElement().satisfies(lesson ->
                assertThat(lesson.messageKey()).isEqualTo("kata.missing-springdoc.lesson.quarkus"));
        assertThat(evaluateBuild(kata, "io.quarkus:quarkus-rest", "io.quarkus:quarkus-smallrye-openapi")).isEmpty();
        assertThat(evaluateBuild(kata, "io.quarkus:quarkus-arc")).isEmpty();
    }
}
