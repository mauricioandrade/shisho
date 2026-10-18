package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.exception.ScaffoldException;
import dev.mauricioandrade.shisho.domain.model.CrudPlan;
import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.GeneratedFile;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.port.in.GenerateCrud;
import dev.mauricioandrade.shisho.infrastructure.build.MavenBuildFileReader;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.config.YamlProjectConfigReader;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import dev.mauricioandrade.shisho.infrastructure.scaffold.TemplateScaffoldRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/** With the real adapters: what matters is how the catalog, the parser and the classifier meet. */
class GenerateCrudServiceTest {

    private static final Path FIXTURES = Path.of("src/test/resources/fixtures");
    private static final MarkdownCatalogRepository CATALOG = MarkdownCatalogRepository.bundled();

    private final GenerateCrudService service = new GenerateCrudService(new MavenBuildFileReader(), new YamlProjectConfigReader(),
            new JavaParserSourceParser(), new ClassifyArchitectureService(CATALOG, Detectors.all()), CATALOG, new TemplateScaffoldRenderer());

    @Test
    void placesTheRequestedArchitectureUnderTheApplicationPackage() {
        CrudPlan plan = service.plan(FIXTURES.resolve("initializr"), request(Optional.of("package-by-feature")));

        assertThat(plan.architecture().id()).isEqualTo("package-by-feature");
        assertThat(plan.files()).extracting(GeneratedFile::file).contains(
                Path.of("src/main/java/com/exemplo/loja/usuario/UsuarioController.java"),
                Path.of("src/main/java/com/exemplo/loja/TratadorDeErros.java"));
        assertThat(plan.skipped()).isEmpty();
        assertThat(plan.otherArchitecture()).isEmpty();
        assertThat(plan.missingDependencies()).isEmpty();
    }

    @Test
    void followsTheDetectedArchitectureAndKeepsTheProjectsErrorHandler(@TempDir Path project) throws IOException {
        copy("aluno-exemplar", project);
        Files.copy(FIXTURES.resolve("initializr/src/main/java/com/exemplo/loja/LojaApplication.java"),
                project.resolve("src/main/java/com/exemplo/loja/LojaApplication.java"));
        CrudPlan pedido = service.plan(project, request("Pedido", Optional.of("package-by-feature")));
        pedido.files().forEach(file -> write(project, file));

        CrudPlan produto = service.plan(project, request("Produto", Optional.empty()));

        assertThat(produto.architecture().id()).as("usuario and pedido are two features").isEqualTo("package-by-feature");
        assertThat(produto.otherArchitecture()).isEmpty();
        assertThat(pedido.skipped()).extracting(GeneratedFile::file).as("the fixture already has a @RestControllerAdvice")
                .containsExactly(Path.of("src/main/java/com/exemplo/loja/TratadorDeErros.java"));
        assertThat(produto.files()).extracting(GeneratedFile::file)
                .contains(Path.of("src/main/java/com/exemplo/loja/produto/ProdutoService.java"));
    }

    @Test
    void warnsWhenTheRequestedArchitectureIsNotTheProjects() {
        CrudPlan plan = service.plan(FIXTURES.resolve("aluno-baguncado"), request(Optional.of("package-by-feature")));

        assertThat(plan.otherArchitecture()).hasValueSatisfying(other -> assertThat(other.id()).isEqualTo("layered"));
    }

    @Test
    void listsTheDependenciesTheBuildLacks(@TempDir Path project) throws IOException {
        Files.writeString(project.resolve("pom.xml"), """
                <project><dependencies><dependency>
                <groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId>
                </dependency></dependencies></project>
                """);

        CrudPlan plan = service.plan(project, new CrudRequest("Usuario", List.of(new CrudRequest.Field("nome", "String")),
                Optional.of("layered"), Optional.of("com.loja"), Optional.empty()));

        assertThat(plan.missingDependencies()).as("Boot 3's web starter stands in for Boot 4's webmvc")
                .containsExactly(List.of("spring-boot-starter-validation"), List.of("spring-boot-starter-data-jpa"));
        assertThat(plan.files()).first().extracting(GeneratedFile::file).isEqualTo(Path.of("src/main/java/com/loja/model/Usuario.java"));
    }

    @Test
    void recognizesTheCrudItGeneratedEvenWithASingleFeature(@TempDir Path project) throws IOException {
        copy("initializr", project);
        service.plan(project, request("Pedido", Optional.of("package-by-feature"))).files().forEach(file -> write(project, file));

        CrudPlan produto = service.plan(project, request("Produto", Optional.empty()));

        assertThat(produto.architecture().id()).as("one feature is not enough for the classifier").isEqualTo("package-by-feature");
        assertThat(produto.otherArchitecture()).isEmpty();
    }

    @Test
    void checksTheCostOfAnArchitectureBeingChosen(@TempDir Path project) throws IOException {
        copy("initializr", project);

        assertThat(service.plan(project, request(Optional.of("layered"))).cost()).as("a simple architecture").isEmpty();
        assertThat(service.plan(project, request(Optional.of("hexagonal"))).cost())
                .hasValueSatisfying(cost -> assertThat(cost.paid()).as("no context declared").isFalse());

        Files.writeString(project.resolve("shisho.yaml"), """
                contexto:
                  tipo: produto
                  caracteristicas: [dominio_com_regras_complexas]
                """);
        assertThat(service.plan(project, request(Optional.of("hexagonal"))).cost())
                .hasValueSatisfying(cost -> assertThat(cost.paidBy()).containsExactly("dominio_com_regras_complexas"));
    }

    @Test
    void aProjectThatAlreadyFollowsTheArchitectureMadeTheDecision(@TempDir Path project) throws IOException {
        copy("initializr", project);
        service.plan(project, request("Pedido", Optional.of("hexagonal"))).files().forEach(file -> write(project, file));

        CrudPlan produto = service.plan(project, request("Produto", Optional.empty()));

        assertThat(produto.architecture().id()).isEqualTo("hexagonal");
        assertThat(produto.cost()).isEmpty();
    }

    @Test
    void asksForAnArchitectureItCannotRecognize() {
        assertThatExceptionOfType(ScaffoldException.class)
                .isThrownBy(() -> service.plan(FIXTURES.resolve("initializr"), request(Optional.empty())))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("crud.error.no-architecture"))
                .satisfies(e -> assertThat(e.arguments()).containsExactly("hexagonal, layered, package-by-feature"));
        assertThatExceptionOfType(ScaffoldException.class)
                .isThrownBy(() -> service.plan(FIXTURES.resolve("initializr"), request(Optional.of("clean"))))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("crud.error.unknown-architecture"));
        assertThatExceptionOfType(ScaffoldException.class)
                .isThrownBy(() -> service.plan(FIXTURES.resolve("aluno-astronauta"), request(Optional.empty())))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("crud.error.no-scaffold"))
                .satisfies(e -> assertThat(e.arguments()).containsExactly("Clean Architecture", "hexagonal, layered, package-by-feature"));
    }

    @Test
    void aQuarkusProjectGetsTheQuarkusTemplateAndKeepsItsExceptionMappers() {
        CrudPlan plan = service.plan(FIXTURES.resolve("aluno-quarkus"), new CrudRequest("Pedido",
                List.of(new CrudRequest.Field("total", "BigDecimal")), Optional.of("package-by-feature"), Optional.of("com.exemplo.loja"),
                Optional.empty()));

        assertThat(plan.files()).extracting(GeneratedFile::file)
                .contains(Path.of("src/main/java/com/exemplo/loja/pedido/PedidoResource.java"))
                .noneMatch(file -> file.toString().endsWith("Controller.java"));
        assertThat(plan.skipped()).extracting(GeneratedFile::file).as("the fixture has a @ServerExceptionMapper method")
                .containsExactly(Path.of("src/main/java/com/exemplo/loja/TratadorDeErros.java"));
        assertThat(plan.missingDependencies()).isEmpty();
    }

    @Test
    void listsTheQuarkusExtensionsTheBuildLacks(@TempDir Path project) throws IOException {
        Files.writeString(project.resolve("pom.xml"), """
                <project><dependencies><dependency>
                <groupId>io.quarkus</groupId><artifactId>quarkus-rest-jackson</artifactId>
                </dependency></dependencies></project>
                """);

        CrudPlan plan = service.plan(project, new CrudRequest("Usuario", List.of(new CrudRequest.Field("nome", "String")),
                Optional.of("layered"), Optional.of("com.loja"), Optional.empty()));

        assertThat(plan.missingDependencies())
                .containsExactly(List.of("quarkus-hibernate-validator"), List.of("quarkus-hibernate-orm-panache"));
        assertThat(plan.files()).extracting(GeneratedFile::file)
                .contains(Path.of("src/main/java/com/loja/resource/UsuarioResource.java"));
    }

    @Test
    void refusesProjectsBelowTheJavaTheTemplatesNeed(@TempDir Path project) throws IOException {
        copy("initializr", project);
        Path pom = project.resolve("pom.xml");
        Files.writeString(pom, Files.readString(pom).replace("<java.version>21<", "<java.version>11<"));

        assertThatExceptionOfType(ScaffoldException.class)
                .isThrownBy(() -> service.plan(project, request(Optional.of("layered"))))
                .satisfies(e -> {
                    assertThat(e.messageKey()).isEqualTo("crud.error.java-version");
                    assertThat(e.arguments()).containsExactly(new JavaVersion(11), GenerateCrud.MINIMUM_JAVA);
                });
    }

    @Test
    void refusesProjectsOnTheJavaxNamespace(@TempDir Path project) throws IOException {
        copy("initializr", project);
        Path entity = Files.createDirectories(project.resolve("src/main/java/com/exemplo/loja/pedido")).resolve("Pedido.java");
        Files.writeString(entity, """
                package com.exemplo.loja.pedido;

                import javax.persistence.Entity;

                @Entity
                class Pedido {}
                """);

        assertThatExceptionOfType(ScaffoldException.class)
                .isThrownBy(() -> service.plan(project, request(Optional.of("layered"))))
                .satisfies(e -> {
                    assertThat(e.messageKey()).isEqualTo("crud.error.refused-import");
                    assertThat(e.arguments()).containsExactly(Path.of("src/main/java/com/exemplo/loja/pedido/Pedido.java"), "javax.persistence.Entity");
                });
    }

    @Test
    void asksForThePackageOfAnEmptyProject(@TempDir Path project) {
        assertThatExceptionOfType(ScaffoldException.class)
                .isThrownBy(() -> service.plan(project, request(Optional.of("layered"))))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo("crud.error.no-package"));
    }

    private static CrudRequest request(Optional<String> architecture) {
        return request("Usuario", architecture);
    }

    private static CrudRequest request(String entity, Optional<String> architecture) {
        return new CrudRequest(entity, List.of(new CrudRequest.Field("nome", "String")), architecture, Optional.empty(), Optional.empty());
    }

    private static void copy(String fixture, Path copy) throws IOException {
        Path source = FIXTURES.resolve(fixture);
        try (var files = Files.walk(source)) {
            for (Path file : files.toList()) {
                Path target = copy.resolve(source.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(file, target);
                }
            }
        }
    }

    private static void write(Path project, GeneratedFile file) {
        try {
            Files.createDirectories(project.resolve(file.file()).getParent());
            Files.writeString(project.resolve(file.file()), file.content());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
