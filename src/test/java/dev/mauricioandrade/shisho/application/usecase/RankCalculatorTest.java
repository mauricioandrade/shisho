package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class RankCalculatorTest {

    private final RankCalculator calculator = new RankCalculator();

    @Test
    void fullScoreWithoutLessons() {
        assertThat(calculator.score(List.of())).isEqualTo(100);
    }

    @Test
    void eachSeverityCostsItsPoints() {
        List<Lesson> lessons = Stream.of(Severity.GRAVE, Severity.MEDIA, Severity.LEVE, Severity.INFO)
                .map(RankCalculatorTest::lesson)
                .toList();

        assertThat(calculator.score(lessons)).isEqualTo(100 - 10 - 4 - 1);
    }

    @Test
    void neverGoesBelowZero() {
        assertThat(calculator.score(Collections.nCopies(11, lesson(Severity.GRAVE)))).isZero();
    }

    private static Lesson lesson(Severity severity) {
        return new Lesson("kata", severity, Path.of("A.java"), 1, "key", List.of());
    }
}
