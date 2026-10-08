package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class FieldInjectionKataTest {

    private final FieldInjectionKata kata = new FieldInjectionKata();

    @Test
    void flagsAutowiredAndInjectFields() {
        assertThat(evaluate(kata, """
                class UserController {
                    @Autowired
                    private UserService service;
                    @jakarta.inject.Inject
                    private Clock clock;
                }
                """)).satisfiesExactly(
                first -> {
                    assertThat(first.line()).isEqualTo(2);
                    assertThat(first.arguments()).containsExactly("service");
                    assertThat(first.messageKey()).isEqualTo("kata.field-injection.lesson");
                },
                second -> assertThat(second.arguments()).containsExactly("clock"));
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
