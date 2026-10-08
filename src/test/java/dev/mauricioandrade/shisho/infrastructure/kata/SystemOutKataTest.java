package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SystemOutKataTest {

    private final SystemOutKata kata = new SystemOutKata();

    @Test
    void flagsConsoleWritesAndPrintStackTrace() {
        assertThat(evaluate(kata, """
                class Importer {
                    void run(List<String> lines) {
                        System.out.println("start");
                        lines.forEach(System.err::println);
                        try {
                            load();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                """)).extracting(Lesson::line, lesson -> lesson.arguments().getFirst()).containsExactly(
                tuple(3, "System.out.println"),
                tuple(4, "System.err.println"),
                tuple(8, "e.printStackTrace"));
    }

    @Test
    void acceptsLoggerAndPassingTheStreamAround() {
        assertThat(evaluate(kata, """
                class Importer {
                    private static final System.Logger LOG = System.getLogger("importer");
                    private final PrintStream out = System.out;

                    void run() {
                        LOG.log(System.Logger.Level.INFO, "start");
                        out.println("report");
                    }
                }
                """)).isEmpty();
    }
}
