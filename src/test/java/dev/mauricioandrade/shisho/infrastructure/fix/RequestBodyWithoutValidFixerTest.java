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
    void keepsAnExistingImport() {
        assertThat(fixed(root, CONTROLLER.replace("import org.springframework.web.bind.annotation.*;",
                "import jakarta.validation.Valid;\nimport org.springframework.web.bind.annotation.*;")))
                .hasValueSatisfying(source -> assertThat(source.split("import jakarta.validation.Valid;", -1)).hasSize(2));
    }

    @Test
    void needsBeanValidationInTheBuild() {
        assertThat(fix(root, "<project><properties><java.version>21</java.version></properties></project>", CONTROLLER)).isEmpty();
    }
}
