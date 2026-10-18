package dev.mauricioandrade.shisho.domain.model;

import dev.mauricioandrade.shisho.domain.model.Baseline.Comparison;
import dev.mauricioandrade.shisho.domain.model.Baseline.Entry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BaselineTest {

    private static final Lesson REPOSITORY = lesson("field-injection", "A.java", 10, "repository");
    private static final Lesson MAILER = lesson("field-injection", "A.java", 12, "mailer");

    @Test
    void aKnownLessonIsRecognizedAfterItsCodeMoves() {
        Baseline baseline = Baseline.of(List.of(REPOSITORY));

        Comparison comparison = baseline.compare(List.of(lesson("field-injection", "A.java", 42, "repository")));

        assertThat(comparison.fresh()).isEmpty();
        assertThat(comparison.known()).extracting(Lesson::line).containsExactly(42);
        assertThat(comparison.fixed()).isZero();
    }

    @Test
    void aLessonWithOtherArgumentsInTheSameFileIsNew() {
        Comparison comparison = Baseline.of(List.of(REPOSITORY)).compare(List.of(REPOSITORY, MAILER));

        assertThat(comparison.fresh()).containsExactly(MAILER);
        assertThat(comparison.known()).containsExactly(REPOSITORY);
    }

    @Test
    void anEntryCoversOneLessonSoTheLaterIdenticalOneIsNew() {
        Lesson first = lesson("system-out", "A.java", 5, "System.out");
        Lesson second = lesson("system-out", "A.java", 9, "System.out");

        Comparison comparison = Baseline.of(List.of(first)).compare(List.of(first, second));

        assertThat(comparison.known()).containsExactly(first);
        assertThat(comparison.fresh()).containsExactly(second);
    }

    @Test
    void countsTheEntriesNoLessonMatchedAsFixed() {
        Comparison comparison = Baseline.of(List.of(REPOSITORY, MAILER)).compare(List.of(MAILER));

        assertThat(comparison.fixed()).isEqualTo(1);
    }

    @Test
    void theSeverityIsNotPartOfTheLesson() {
        Comparison comparison = Baseline.of(List.of(REPOSITORY)).compare(List.of(REPOSITORY.withSeverity(Severity.LEVE)));

        assertThat(comparison.fresh()).isEmpty();
    }

    @Test
    void recordsPathsWithSlashesAndArgumentsAsTextInAStableOrder() {
        Baseline baseline = Baseline.of(List.of(
                lesson("system-out", "b/B.java", 1, "System.out"),
                new Lesson("prefer-configuration-properties", Severity.LEVE, Path.of("a", "A.java"), 3,
                        "kata.prefer-configuration-properties.lesson", List.of("loja.frete", 3))));

        assertThat(baseline.entries()).containsExactly(
                new Entry("prefer-configuration-properties", "a/A.java", "kata.prefer-configuration-properties.lesson",
                        List.of("loja.frete", "3")),
                new Entry("system-out", "b/B.java", "kata.system-out.lesson", List.of("System.out")));
    }

    private static Lesson lesson(String kata, String file, int line, String argument) {
        return new Lesson(kata, Severity.GRAVE, Path.of(file), line, "kata." + kata + ".lesson", List.of(argument));
    }
}
