package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile.Parsed;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/** Every kata against whole projects: the two fixtures of the acceptance criteria and the Shishō itself. */
class KatasTest {

    private static final Path FIXTURES = Path.of("src/test/resources/fixtures");

    @Test
    void katasHaveUniqueIds() {
        assertThat(Katas.all()).extracting(Kata::id).doesNotHaveDuplicates().hasSize(7);
    }

    @Test
    void messyStudentGetsOneLessonPerViolation() {
        String controller = "src/main/java/com/exemplo/loja/controller/UsuarioController.java";
        String request = "src/main/java/com/exemplo/loja/dto/UsuarioRequest.java";
        String pedido = "src/main/java/com/exemplo/loja/service/PedidoService.java";
        String usuario = "src/main/java/com/exemplo/loja/service/UsuarioService.java";

        assertThat(analyze(FIXTURES.resolve("aluno-baguncado")))
                .extracting(lesson -> lesson.file().toString(), Lesson::line, Lesson::kataId, Lesson::severity)
                .containsExactly(
                        tuple(controller, 12, "field-injection", Severity.GRAVE),
                        tuple(controller, 16, "request-body-without-valid", Severity.MEDIA),
                        tuple(controller, 17, "system-out", Severity.MEDIA),
                        tuple(request, 3, "dto-not-record", Severity.LEVE),
                        tuple(pedido, 9, "non-final-dependency", Severity.MEDIA),
                        tuple(usuario, 10, "lombok-constructor", Severity.MEDIA),
                        tuple(usuario, 13, "non-final-dependency", Severity.MEDIA),
                        tuple(usuario, 15, "value-outside-constructor", Severity.LEVE),
                        tuple(usuario, 22, "system-out", Severity.MEDIA));
    }

    @Test
    void modelStudentHasNothingToLearn() {
        assertThat(analyze(FIXTURES.resolve("aluno-exemplar"))).isEmpty();
    }

    @Test
    void shishoPassesItsOwnKatas() {
        assertThat(analyze(Path.of(""))).isEmpty();
    }

    /** Ordered by file, then line, then kata order — the same order the report will use. */
    private static List<Lesson> analyze(Path project) {
        List<Kata> katas = Katas.all();
        return new JavaParserSourceParser().parse(project, new JavaVersion(25)).stream()
                .map(Parsed.class::cast)
                .flatMap(parsed -> parsed.classes().stream())
                .flatMap(unit -> katas.stream().flatMap(kata -> kata.evaluate(unit).stream()))
                .sorted(Comparator.comparing((Lesson lesson) -> lesson.file().toString())
                        .thenComparingInt(Lesson::line))
                .toList();
    }
}
