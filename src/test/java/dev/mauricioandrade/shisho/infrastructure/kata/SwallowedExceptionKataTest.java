package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SwallowedExceptionKataTest {

    private final SwallowedExceptionKata kata = new SwallowedExceptionKata();

    @Test
    void flagsEmptyCatchBlocks() {
        assertThat(evaluate(kata, """
                class Loader {
                    void load() {
                        try {
                            read();
                        } catch (IOException | IllegalStateException e) {
                        }
                        Runnable later = () -> {
                            try { read(); } catch (Exception e) {}
                        };
                    }
                }
                """)).extracting(Lesson::line, lesson -> lesson.arguments().getFirst()).containsExactly(
                tuple(5, "IOException | IllegalStateException"),
                tuple(8, "Exception"));
    }

    @Test
    void acceptsHandledAndDeliberatelyIgnoredExceptions() {
        assertThat(evaluate(kata, """
                class Loader {
                    void load() {
                        try { read(); } catch (IOException e) { log.warn("read failed", e); }
                        try { read(); } catch (IOException e) { throw new UncheckedIOException(e); }
                        try { close(); } catch (IOException ignored) {}
                        try { parse(); } catch (NumberFormatException expected) {}
                        try { close(); } catch (Exception _) {}
                        try {
                            createHold();
                        } catch (DuplicateKeyException ex) {
                            // idempotent: the hold is already there
                        }
                    }
                }
                """)).isEmpty();
    }
}
