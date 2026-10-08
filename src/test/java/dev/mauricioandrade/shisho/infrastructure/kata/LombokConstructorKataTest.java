package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class LombokConstructorKataTest {

    private final LombokConstructorKata kata = new LombokConstructorKata();

    @Test
    void flagsLombokConstructors() {
        assertThat(evaluate(kata, """
                @Service
                @RequiredArgsConstructor
                class UserService {}

                @lombok.AllArgsConstructor
                class OrderService {}
                """)).extracting(Lesson::line, lesson -> lesson.arguments().getFirst())
                .containsExactly(
                        tuple(2, "RequiredArgsConstructor"),
                        tuple(5, "AllArgsConstructor"));
    }

    @Test
    void acceptsExplicitConstructorAndOtherLombokAnnotations() {
        assertThat(evaluate(kata, """
                @Getter
                @NoArgsConstructor
                class UserService {
                    private final UserRepository repository;

                    UserService(UserRepository repository) {
                        this.repository = repository;
                    }
                }
                """)).isEmpty();
    }
}
