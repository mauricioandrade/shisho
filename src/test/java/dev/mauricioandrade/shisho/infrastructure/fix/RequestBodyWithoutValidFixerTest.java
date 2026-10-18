package dev.mauricioandrade.shisho.infrastructure.fix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static dev.mauricioandrade.shisho.infrastructure.fix.FixTestSupport.fix;
import static dev.mauricioandrade.shisho.infrastructure.fix.FixTestSupport.fixed;
import static org.assertj.core.api.Assertions.assertThat;

class RequestBodyWithoutValidFixerTest {

    private static final String CONTROLLER = """
            package app;

            import org.springframework.web.bind.annotation.*;

            @RestController
            class OrderController {

                @PostMapping
                OrderResponse create(@RequestBody CreateOrder request) {
                    return null;
                }
            }
            """;

    @TempDir
    Path root;

    @Test
    void addsValidAndItsImport() {
        assertThat(fixed(root, CONTROLLER)).contains("""
                package app;

                import org.springframework.web.bind.annotation.*;
                import jakarta.validation.Valid;

                @RestController
                class OrderController {

                    @PostMapping
                    OrderResponse create(@Valid @RequestBody CreateOrder request) {
                        return null;
                    }
                }
                """);
    }

    @Test
    void importsJavaxValidOnSpringBoot2() {
        FixTestSupport.write(root, "src/main/java/app/Order.java", """
                package app;

                import javax.persistence.Entity;

                @Entity
                class Order {}
                """);

        assertThat(fixed(root, CONTROLLER)).as("javax.persistence: Spring Boot 2, where jakarta.validation does not exist")
                .hasValueSatisfying(source -> assertThat(source).contains("import javax.validation.Valid;").doesNotContain("jakarta"));
    }

    @Test
    void keepsAnExistingImport() {
        assertThat(fixed(root, CONTROLLER.replace("import org.springframework.web.bind.annotation.*;",
                "import jakarta.validation.Valid;\nimport org.springframework.web.bind.annotation.*;")))
                .hasValueSatisfying(source -> assertThat(source.split("import jakarta.validation.Valid;", -1)).hasSize(2));
    }

    @Test
    void addsValidToAJakartaRestBody() {
        String pom = """
                <project>
                    <properties><maven.compiler.release>21</maven.compiler.release></properties>
                    <dependencies>
                        <dependency>
                            <groupId>io.quarkus</groupId>
                            <artifactId>quarkus-hibernate-validator</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;
        assertThat(fix(root, pom, """
                package app;

                import jakarta.ws.rs.*;

                @Path("/orders")
                public class OrderResource {

                    @POST
                    @Path("{id}")
                    public OrderResponse update(Long id, UpdateOrder request) {
                        return null;
                    }
                }
                """)).singleElement().satisfies(fix -> assertThat(fix.fixed())
                .contains("import jakarta.validation.Valid;")
                .contains("public OrderResponse update(Long id, @Valid UpdateOrder request) {"));
    }

    @Test
    void needsBeanValidationInTheBuild() {
        assertThat(fix(root, "<project><properties><java.version>21</java.version></properties></project>", CONTROLLER)).isEmpty();
    }
}
