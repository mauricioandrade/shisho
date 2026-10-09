package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class MissingControllerAdviceKataTest {

    private final MissingControllerAdviceKata kata = new MissingControllerAdviceKata();

    @Test
    void flagsTheFirstControllerOfAProjectWithoutAdvice() {
        assertThat(evaluate(kata, """
                @Service
                class UserService {}

                @RestController
                class UserController {}

                @Controller
                class PageController {}
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(4);
            assertThat(lesson.arguments()).containsExactly("UserController");
            assertThat(lesson.messageKey()).isEqualTo("kata.missing-controller-advice.lesson");
        });
    }

    @Test
    void acceptsEitherKindOfAdvice() {
        assertThat(evaluate(kata, """
                @RestController
                class UserController {}

                @org.springframework.web.bind.annotation.ControllerAdvice
                class Errors {}
                """)).isEmpty();
    }

    @Test
    void doesNothingWithoutControllers() {
        assertThat(evaluate(kata, """
                @Service
                class UserService {}
                """)).isEmpty();
    }
}
