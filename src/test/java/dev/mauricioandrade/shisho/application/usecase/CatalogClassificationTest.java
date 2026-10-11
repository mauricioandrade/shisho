package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.SourceFile.Parsed;
import dev.mauricioandrade.shisho.infrastructure.build.MavenBuildFileReader;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** The real catalog against real projects: the fixtures and the Shishō itself. */
class CatalogClassificationTest {

    private final ClassifyArchitectureService service = new ClassifyArchitectureService(MarkdownCatalogRepository.bundled(), Detectors.all());

    @Test
    void theShishoIsCleanArchitecture() {
        assertThat(classify(Path.of(""))).hasValueSatisfying(match -> {
            assertThat(match.architecture().id()).isEqualTo("clean");
            assertThat(match.confidence()).isEqualTo(100);
        });
    }

    @Test
    void theMessyStudentIsLayered() {
        assertThat(classify(Path.of("src/test/resources/fixtures/aluno-baguncado")))
                .hasValueSatisfying(match -> assertThat(match.architecture().id()).isEqualTo("layered"));
    }

    @Test
    void aSinglePackageResemblesNothing() {
        assertThat(classify(Path.of("src/test/resources/fixtures/aluno-exemplar"))).isEmpty();
    }

    @Test
    void quarkusResourcesAndPanacheActiveRecordAreLayered(@TempDir Path root) throws IOException {
        write(root, "com/loja/resource/PedidoResource.java", """
                package com.loja.resource;

                import com.loja.service.PedidoService;

                @Path("/pedidos")
                public class PedidoResource {
                    @Inject
                    PedidoService service;
                }
                """);
        write(root, "com/loja/service/PedidoService.java", """
                package com.loja.service;

                import com.loja.entity.Pedido;

                @ApplicationScoped
                public class PedidoService {
                    Pedido buscar(Long id) { return Pedido.findById(id); }
                }
                """);
        write(root, "com/loja/entity/Pedido.java", """
                package com.loja.entity;

                @Entity
                public class Pedido extends PanacheEntity {
                    public String cliente;
                }
                """);

        assertThat(classify(root)).hasValueSatisfying(match -> assertThat(match.architecture().id()).isEqualTo("layered"));
    }

    @Test
    void quarkusResourcesByFeatureArePackageByFeature(@TempDir Path root) throws IOException {
        for (String feature : List.of("pedido", "cliente")) {
            String type = Character.toUpperCase(feature.charAt(0)) + feature.substring(1);
            write(root, "com/loja/" + feature + "/" + type + "Resource.java",
                    "package com.loja." + feature + ";\n@Path(\"/" + feature + "\")\npublic class " + type + "Resource {}\n");
            write(root, "com/loja/" + feature + "/" + type + "Service.java",
                    "package com.loja." + feature + ";\n@ApplicationScoped\nclass " + type + "Service {}\n");
        }

        assertThat(classify(root)).hasValueSatisfying(match ->
                assertThat(match.architecture().id()).isEqualTo("package-by-feature"));
    }

    private static void write(Path root, String file, String source) throws IOException {
        Path path = root.resolve("src/main/java").resolve(file);
        Files.createDirectories(path.getParent());
        Files.writeString(path, source);
    }

    private Optional<ArchitectureMatch> classify(Path root) {
        var classes = new JavaParserSourceParser().parse(root, new JavaVersion(25)).stream()
                .map(Parsed.class::cast)
                .flatMap(parsed -> parsed.classes().stream())
                .toList();
        return service.classify(new ProjectModel(new JavaVersion(25), new MavenBuildFileReader().read(root), classes), Optional.empty());
    }
}
