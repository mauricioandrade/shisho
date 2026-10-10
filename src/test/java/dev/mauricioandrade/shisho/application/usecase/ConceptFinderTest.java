package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Concept;
import dev.mauricioandrade.shisho.domain.model.Concept.Group;
import dev.mauricioandrade.shisho.domain.model.Concept.Signal;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding;
import dev.mauricioandrade.shisho.domain.model.ConceptFinding.Occurrence;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Snippet;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.SourceFile.Parsed;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.concept.ConceptDetectors;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class ConceptFinderTest {

    private static final Path SAMPLE = Path.of("src/main/java/Sample.java");

    @TempDir
    Path project;

    @TestFactory
    Stream<DynamicTest> everyCatalogConceptRecognizesItsOwnExample() {
        return Stream.of(Optional.<String>empty(), Optional.of("en"))
                .flatMap(translation -> MarkdownCatalogRepository.bundled(translation).concepts().stream()
                        .map(concept -> DynamicTest.dynamicTest(concept.id() + translation.map(code -> " (" + code + ")").orElse(""), () -> {
                            assertThat(concept.example().language()).isEqualTo("java");
                            assertThat(find(concept, concept.example().code())).as(concept.example().code())
                                    .singleElement().extracting(ConceptFinding::conceptId).isEqualTo(concept.id());
                        })));
    }

    @Test
    void conditionsHoldOnTheSameType() throws IOException {
        Concept repository = concept(new Signal(Optional.empty(), Optional.of("interface"), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of("Repository"), Optional.empty()));

        List<ConceptFinding> found = find(repository, """
                class PedidoRepository {
                }

                interface Pedidos {
                }

                interface ClienteRepository {
                }
                """);

        assertThat(found).singleElement().extracting(ConceptFinding::occurrences)
                .isEqualTo(List.of(new Occurrence(SAMPLE, 7)));
    }

    @Test
    void pointsAtTheAnnotationTheCallOrTheType() throws IOException {
        Concept transactional = concept(signal(Optional.of("Transactional"), Optional.empty()));
        Concept streams = concept(signal(Optional.empty(), Optional.of("stream")));

        String code = """
                class PedidoService {

                    @Transactional
                    void criar() {
                    }

                    List<String> nomes(List<Pedido> pedidos) {
                        return pedidos.stream().map(Pedido::nome).toList();
                    }
                }
                """;

        assertThat(find(transactional, code).getFirst().occurrences()).containsExactly(new Occurrence(SAMPLE, 3));
        assertThat(find(streams, code).getFirst().occurrences()).containsExactly(new Occurrence(SAMPLE, 8));
    }

    @Test
    void skipsADetectorThatDoesNotExistYet() throws IOException {
        Concept future = concept(new Signal(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.of("ainda_nao_existe")));

        assertThat(find(future, "class Qualquer {\n}\n")).isEmpty();
    }

    private List<ConceptFinding> find(Concept concept, String code) throws IOException {
        Path file = Files.createDirectories(project.resolve("src/main/java")).resolve("Sample.java");
        Files.writeString(file, code);
        List<ClassUnit> classes = new JavaParserSourceParser().parse(project, new JavaVersion(25)).stream()
                .map(Parsed.class::cast)
                .flatMap(parsed -> parsed.classes().stream())
                .toList();
        return new ConceptFinder(List.of(concept), ConceptDetectors.all())
                .find(new ProjectModel(new JavaVersion(25), Optional.empty(), classes));
    }

    private static Signal signal(Optional<String> annotation, Optional<String> call) {
        return new Signal(annotation, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), call,
                Optional.empty(), Optional.empty());
    }

    private static Concept concept(Signal signal) {
        return new Concept("x", "X", Group.JAVA, List.of(signal), List.of("https://x.dev"), List.of("X."),
                new Snippet("java", "class X {}", ""));
    }
}
