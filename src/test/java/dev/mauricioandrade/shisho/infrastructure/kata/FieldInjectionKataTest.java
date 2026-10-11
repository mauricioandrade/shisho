package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class FieldInjectionKataTest {

    private static final String INJECTED_FIELDS = """
            class UserController {
                @Autowired
                private UserService service;
                @jakarta.inject.Inject
                Clock clock;
            }
            """;

    private final FieldInjectionKata kata = new FieldInjectionKata();

    @Test
    void flagsAutowiredAndInjectFields() {
        assertThat(evaluate(kata, INJECTED_FIELDS)).satisfiesExactly(
                first -> {
                    assertThat(first.line()).isEqualTo(2);
                    assertThat(first.arguments()).containsExactly("service");
                    assertThat(first.messageKey()).isEqualTo("kata.field-injection.lesson");
                },
                second -> assertThat(second.arguments()).containsExactly("clock"));
    }

    @Test
    void flagsInjectFieldsInSpringBootApplications() {
        assertThat(evaluate(kata, INJECTED_FIELDS, "org.springframework.boot:spring-boot-starter-web")).hasSize(2);
    }

    @Test
    void acceptsInjectFieldsInQuarkusApplications() {
        assertThat(evaluate(kata, INJECTED_FIELDS, "io.quarkus:quarkus-arc", "io.quarkus:quarkus-rest"))
                .singleElement()
                .satisfies(lesson -> assertThat(lesson.arguments()).containsExactly("service"));
    }

    @Test
    void acceptsConstructorInjection() {
        assertThat(evaluate(kata, """
                class UserController {
                    private final UserService service;

                    @Autowired
                    UserController(UserService service) {
                        this.service = service;
                    }
                }
                """)).isEmpty();
    }
}
