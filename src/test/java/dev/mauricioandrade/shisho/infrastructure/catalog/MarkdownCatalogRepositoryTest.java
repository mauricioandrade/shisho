package dev.mauricioandrade.shisho.infrastructure.catalog;

import dev.mauricioandrade.shisho.domain.exception.CatalogException;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.DependencyRule;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.AvoidedDependency;
import dev.mauricioandrade.shisho.domain.model.Concept;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaFeature;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Snippet;
import dev.mauricioandrade.shisho.domain.model.ProjectContext;
import dev.mauricioandrade.shisho.domain.model.Scaffold;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownCatalogRepositoryTest {

    private static final String VALID_FRONTMATTER = """
            id: sample
            nome: Sample
            familia: simples
            assinatura:
              pacotes: [controller, service]
              minimo_sinais: 2
            regras_dependencia:
              - de: service
                nao_pode_depender_de: [controller]
            custo:
              classes_por_feature: 4-6
              complexidade: baixa
            faz_sentido_quando: [crud_simples]
            sinais_overengineering: []
            sinais_subengenharia: [service_gigante]
            evolui_para: []
            alternativa_mais_simples: null
            referencias: [https://guide.com]
            """;

    @Nested
    class RealCatalog {

        private final List<ArchitectureProfile> architectures =
                new MarkdownCatalogRepository(Path.of("catalog")).architectures();

        @Test
        void loadsEveryArchitectureInTheRepository() {
            assertThat(architectures).extracting(ArchitectureProfile::id).containsExactly(
                    "clean", "cqrs", "event-driven", "hexagonal", "layered",
                    "microservices", "modular-monolith", "onion", "package-by-feature");
        }

        @Test
        void everyEvolutionAndSimplerAlternativePointsToAnExistingArchitecture() {
            Set<String> ids = architectures.stream().map(ArchitectureProfile::id).collect(Collectors.toSet());

            for (ArchitectureProfile architecture : architectures) {
                assertThat(ids).as("evolui_para de %s", architecture.id()).containsAll(architecture.evolvesTo());
                architecture.simplerAlternative().ifPresent(alternative ->
                        assertThat(ids).as("alternativa_mais_simples de %s", architecture.id()).contains(alternative));
            }
        }

        @Test
        void everyArchitectureHasAnExplanation() {
            assertThat(architectures).allSatisfy(architecture ->
                    assertThat(architecture.markdown()).as(architecture.id()).startsWith("## "));
        }

        @Test
        void mapsHexagonalFields() {
            ArchitectureProfile hexagonal = architectures.stream()
                    .filter(architecture -> architecture.id().equals("hexagonal"))
                    .findFirst()
                    .orElseThrow();

            assertThat(hexagonal.name()).isEqualTo("Hexagonal (Ports & Adapters)");
            assertThat(hexagonal.family()).isEqualTo(Family.DOMAIN_ISOLATION);
            assertThat(hexagonal.signature().packages()).contains("port|porta|portas/in|entrada", "adapter|adaptador|adaptadores/out|saida");
            assertThat(hexagonal.signature().annotations()).isEmpty();
            assertThat(hexagonal.signature().minimumSignals()).isEqualTo(2);
            assertThat(hexagonal.dependencyRules()).containsExactly(
                    new DependencyRule("domain|dominio",
                            List.of("adapter|adaptador|adaptadores", "infrastructure|infraestrutura", "org.springframework", "jakarta.persistence")),
                    new DependencyRule("application|aplicacao", List.of("adapter|adaptador|adaptadores")));
            assertThat(hexagonal.cost().classesPerFeature()).isEqualTo(new ClassRange(6, 10));
            assertThat(hexagonal.cost().complexity()).isEqualTo(Complexity.MEDIUM);
            assertThat(hexagonal.simplerAlternative()).contains("package-by-feature");
        }

        @Test
        void everyContextKeyIsOneTheProjectCanDeclare() {
            assertThat(architectures).flatExtracting(ArchitectureProfile::makesSenseWhen)
                    .allSatisfy(key -> assertThat(ProjectContext.KNOWN_KEYS).contains(key));
        }

        @Test
        void theBundledCatalogIsTheRepositoryCatalog() {
            assertThat(MarkdownCatalogRepository.bundled().architectures()).isEqualTo(architectures);
        }

        @Test
        void layeredHasNoSimplerAlternative() {
            assertThat(architectures)
                    .filteredOn(architecture -> architecture.id().equals("layered"))
                    .singleElement()
                    .satisfies(layered -> assertThat(layered.simplerAlternative()).isEmpty());
        }
    }

    @Nested
    class KataGuides {

        @TempDir
        Path catalog;

        @Test
        void readsTheExplanationTheCodeBeforeAndAfterAndTheReferences() throws IOException {
            write("system-out.md", """
                    ---
                    id: system-out
                    referencias:
                      - https://openjdk.org/jeps/264
                    ---

                    ## Por quê

                    O console não tem nível
                    nem formato.

                    Um logger tem.

                    ## Quando é aceitável

                    Numa CLI.

                    ## Antes

                    ```java
                    System.out.println("x");
                    ```

                    ## Depois

                    O logger tem nível
                    e formato.

                    ```java
                    log.info("x");

                    log.debug("y");
                    ```
                    """);

            assertThat(new MarkdownCatalogRepository(catalog).kataGuides()).containsExactly(new KataGuide("system-out",
                    List.of("O console não tem nível nem formato.", "Um logger tem."),
                    List.of("Numa CLI."),
                    new Snippet("java", "System.out.println(\"x\");", ""),
                    new Snippet("java", "log.info(\"x\");\n\nlog.debug(\"y\");", "O logger tem nível e formato."),
                    List.of("https://openjdk.org/jeps/264")));
        }

        @Test
        void rejectsAGuideWithoutTheExplanationOrTheReferences() throws IOException {
            String code = "## Antes\n\n```java\nx();\n```\n\n## Depois\n\n```java\ny();\n```\n";
            write("system-out.md", "---\nid: system-out\nreferencias: [https://x.dev]\n---\n\n## Quando é aceitável\n\nNunca.\n\n" + code);
            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).kataGuides())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.arguments()).containsExactly("system-out.md", "## Por quê"));

            write("system-out.md", "---\nid: system-out\nreferencias: []\n---\n\n## Por quê\n\nX.\n\n## Quando é aceitável\n\nY.\n\n" + code);
            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).kataGuides())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.messageKey()).isEqualTo("catalog.error.invalid-value"));
        }

        @Test
        void rejectsAGuideWithoutBothCodeSectionsOrCode() throws IOException {
            String prose = "---\nid: system-out\nreferencias: [https://x.dev]\n---\n\n## Por quê\n\nX.\n\n## Quando é aceitável\n\nY.\n\n";
            write("system-out.md", prose + "## Antes\n\n```java\nx();\n```\n");
            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).kataGuides())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.arguments()).containsExactly("system-out.md", "## Depois"));

            write("system-out.md", prose + "## Antes\n\nsó texto\n\n## Depois\n\n```java\nx();\n```\n");
            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).kataGuides())
                    .isInstanceOfSatisfying(CatalogException.class, e -> {
                        assertThat(e.messageKey()).isEqualTo("catalog.error.missing-code");
                        assertThat(e.arguments()).containsExactly("system-out.md", "Antes");
                    });
        }

        @Test
        void rejectsAnIdDifferentFromTheFileName() throws IOException {
            write("system-out.md", "---\nid: sistema\n---\n");

            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).kataGuides())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.messageKey()).isEqualTo("catalog.error.id-mismatch"));
        }

        private void write(String fileName, String content) throws IOException {
            Files.writeString(Files.createDirectories(catalog.resolve("katas")).resolve(fileName), content);
        }
    }

    @Nested
    class JavaFeatures {

        @Test
        void readsTheRealCatalog() {
            List<JavaFeature> features = MarkdownCatalogRepository.bundled().javaFeatures();

            assertThat(features).extracting(JavaFeature::id).contains("virtual-threads", "scoped-values", "structured-concurrency");
            assertThat(features).filteredOn(feature -> feature.id().equals("scoped-values")).singleElement().satisfies(feature -> {
                assertThat(feature.name()).isEqualTo("Scoped values");
                assertThat(feature.since()).isEqualTo(new JavaVersion(25));
                assertThat(feature.status()).isEqualTo(JavaFeature.Status.FINAL);
                assertThat(feature.jep()).isEqualTo(506);
                assertThat(feature.markdown()).startsWith("## O que é");
            });
            assertThat(features).filteredOn(feature -> feature.id().equals("structured-concurrency"))
                    .extracting(JavaFeature::status).containsExactly(JavaFeature.Status.PREVIEW);
        }

        @Test
        void rejectsAnUnknownStatus(@TempDir Path catalog) throws IOException {
            Files.writeString(Files.createDirectories(catalog.resolve("java-features")).resolve("x.md"),
                    "---\nid: x\nnome: X\ndesde: 25\nstatus: beta\njep: 1\n---\n");

            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).javaFeatures())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.arguments()).containsExactly("x.md", "status", "beta"));
        }
    }

    @Nested
    class AvoidedDependencies {

        @Test
        void readsTheRealCatalog() {
            List<AvoidedDependency> avoided = MarkdownCatalogRepository.bundled().avoidedDependencies();

            assertThat(avoided).extracting(AvoidedDependency::id).contains("log4j-1", "springfox", "junit-4");
            assertThat(avoided).allSatisfy(dependency -> {
                assertThat(dependency.references()).as(dependency.id()).allMatch(url -> url.startsWith("https://"));
                assertThat(dependency.markdown()).as(dependency.id()).startsWith("## Por quê");
            });
            assertThat(avoided).filteredOn(dependency -> dependency.id().equals("springfox")).singleElement()
                    .satisfies(springfox -> assertThat(springfox.coordinates())
                            .contains(new Dependency("io.springfox", "springfox-boot-starter")));
        }

        @Test
        void neverAdvisesAgainstTheSameLibraryTwice() {
            assertThat(MarkdownCatalogRepository.bundled().avoidedDependencies().stream()
                    .flatMap(dependency -> dependency.coordinates().stream())).doesNotHaveDuplicates();
        }

        @Test
        void rejectsCoordinatesThatAreNotGroupAndArtifact(@TempDir Path catalog) throws IOException {
            Files.writeString(Files.createDirectories(catalog.resolve("dependencies")).resolve("x.md"),
                    "---\nid: x\nnome: X\ncoordenadas: [log4j]\nresumo: r\nalternativa: a\nreferencias: [https://x.dev]\n---\n");

            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).avoidedDependencies())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.arguments()).containsExactly("x.md", "coordenadas", "log4j"));
        }
    }

    @Nested
    class Concepts {

        @Test
        void readsTheRealCatalog() {
            List<Concept> concepts = MarkdownCatalogRepository.bundled().concepts();

            assertThat(concepts).extracting(Concept::id).contains("record", "transacoes", "strategy").doesNotHaveDuplicates();
            assertThat(concepts).extracting(Concept::group).contains(Concept.Group.values());
            assertThat(concepts).extracting(Concept::id).as("by group, then in the order a beginner meets them")
                    .startsWith("record", "enum").containsSubsequence("beans", "injecao-de-dependencia", "cdi-beans", "jakarta-rest", "entidade-jpa", "transacoes",
                            "bean-validation", "repository", "active-record", "dto");
            assertThat(concepts).allSatisfy(concept -> {
                assertThat(concept.explanation()).as(concept.id()).isNotEmpty();
                assertThat(concept.references()).as(concept.id()).allMatch(url -> url.startsWith("https://"));
            });
        }

        @Test
        void rejectsAConditionItDoesNotKnow(@TempDir Path catalog) throws IOException {
            Files.writeString(Files.createDirectories(catalog.resolve("concepts")).resolve("x.md"), """
                    ---
                    id: x
                    nome: X
                    grupo: java
                    ordem: 10
                    aparece_em:
                      - anotaçao: Service
                    referencias: [https://x.dev]
                    ---

                    ## O que é

                    X.

                    ## Exemplo

                    ```java
                    class X {}
                    ```
                    """);

            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).concepts())
                    .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.arguments()).contains("x.md", "aparece_em[0].anotaçao"));
        }
    }

    @Nested
    class Translations {

        private static final String ORIGINAL = """
                ---
                id: x
                nome: Biblioteca X
                coordenadas: ["com.x:x"]
                resumo: abandonada
                alternativa: a biblioteca padrão
                referencias: [https://x.dev]
                ---

                ## Por quê

                Texto em português.
                """;

        @TempDir
        Path catalog;

        @Test
        void replaceTheTextAndKeepTheData() throws IOException {
            write("dependencies/x.md", ORIGINAL);
            write("en/dependencies/x.md", """
                    ---
                    id: x
                    nome: Library X
                    resumo: abandoned
                    alternativa: the standard library
                    ---

                    ## Why

                    English text.
                    """);

            AvoidedDependency x = new MarkdownCatalogRepository(catalog, Optional.of("en")).avoidedDependencies().getFirst();

            assertThat(x.name()).isEqualTo("Library X");
            assertThat(x.summary()).isEqualTo("abandoned");
            assertThat(x.alternative()).isEqualTo("the standard library");
            assertThat(x.markdown()).isEqualTo("## Why\n\nEnglish text.");
            assertThat(x.coordinates()).containsExactly(new Dependency("com.x", "x"));
            assertThat(x.references()).containsExactly("https://x.dev");
        }

        @Test
        void neverCarryData() throws IOException {
            write("dependencies/x.md", ORIGINAL);
            write("en/dependencies/x.md", "---\nid: x\ncoordenadas: [\"com.y:y\"]\n---\n");

            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog, Optional.of("en")).avoidedDependencies())
                    .isInstanceOfSatisfying(CatalogException.class, e -> assertThat(e.arguments()).contains("x.md", "coordenadas"));
        }

        @Test
        void fallBackToTheOriginal() throws IOException {
            write("dependencies/x.md", ORIGINAL);

            assertThat(new MarkdownCatalogRepository(catalog, Optional.of("en")).avoidedDependencies().getFirst().name())
                    .isEqualTo("Biblioteca X");
        }

        @Test
        void theRepositoryCatalogIsTranslatedWholeAndOnlyThere() throws IOException {
            for (String folder : List.of("architectures", "katas", "java-features", "dependencies", "scaffolds", "concepts")) {
                assertThat(fileNames(Path.of("catalog", "en", folder))).as(folder).isEqualTo(fileNames(Path.of("catalog", folder)));
            }
            MarkdownCatalogRepository english = MarkdownCatalogRepository.bundled(Optional.of("en"));
            assertThat(english.architectures()).extracting(ArchitectureProfile::name).contains("Technical layers (Layered)");
            assertThat(english.javaFeatures()).isNotEmpty();
            assertThat(english.avoidedDependencies()).isNotEmpty();
            assertThat(english.scaffolds()).allSatisfy(scaffold -> assertThat(scaffold.files())
                    .anySatisfy(file -> assertThat(file.path()).endsWith("ErrorHandler.java")));
        }

        private static List<String> fileNames(Path directory) throws IOException {
            try (Stream<Path> files = Files.list(directory)) {
                return files.map(file -> file.getFileName().toString()).sorted().toList();
            }
        }

        private void write(String file, String content) throws IOException {
            Path path = catalog.resolve(file);
            Files.createDirectories(path.getParent());
            Files.writeString(path, content);
        }
    }

    @Nested
    class Scaffolds {

        @Test
        void everyScaffoldFollowsACatalogArchitecture() {
            MarkdownCatalogRepository catalog = MarkdownCatalogRepository.bundled();
            Set<String> architectures = catalog.architectures().stream().map(ArchitectureProfile::id).collect(Collectors.toSet());

            assertThat(catalog.scaffolds()).extracting(Scaffold::architectureId).containsExactly("hexagonal", "layered", "package-by-feature")
                    .allMatch(architectures::contains);
            assertThat(catalog.scaffolds()).allSatisfy(scaffold -> assertThat(scaffold.files()).allSatisfy(file -> {
                assertThat(file.path()).as(scaffold.architectureId()).endsWith(".java");
                assertThat(file.explanation()).as(file.path()).isNotBlank();
                assertThat(file.content()).as(file.path()).startsWith("package {{pacote}}").endsWith("}\n");
            }));
        }

        @Test
        void readsTheFilesSharedByEveryEntity(@TempDir Path catalog) throws IOException {
            Files.writeString(Files.createDirectories(catalog.resolve("scaffolds")).resolve("x.md"), """
                    ---
                    id: x
                    dependencias: ["spring-boot-starter-webmvc|spring-boot-starter-web"]
                    uma_vez_por_projeto:
                      Erros.java: RestControllerAdvice
                    ---

                    ## {{Entidade}}.java

                    A entidade.

                    ```java
                    class {{Entidade}} {}
                    ```

                    ## Erros.java

                    Os erros.

                    ```java
                    class Erros {}
                    ```
                    """);

            assertThat(new MarkdownCatalogRepository(catalog).scaffolds()).singleElement().satisfies(scaffold -> {
                assertThat(scaffold.requiredDependencies()).containsExactly(List.of("spring-boot-starter-webmvc", "spring-boot-starter-web"));
                assertThat(scaffold.files()).containsExactly(
                        new Scaffold.File("{{Entidade}}.java", "A entidade.", "class {{Entidade}} {}\n", Optional.empty()),
                        new Scaffold.File("Erros.java", "Os erros.", "class Erros {}\n", Optional.of("RestControllerAdvice")));
            });
        }

        @Test
        void rejectsASharedFileThatIsNotInTheTemplate(@TempDir Path catalog) throws IOException {
            Files.writeString(Files.createDirectories(catalog.resolve("scaffolds")).resolve("x.md"),
                    "---\nid: x\ndependencias: []\numa_vez_por_projeto:\n  Outro.java: X\n---\n\n## A.java\n\nA.\n\n```java\nclass A {}\n```\n");

            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).scaffolds())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.arguments()).containsExactly("x.md", "uma_vez_por_projeto", "Outro.java"));
        }
    }

    @Nested
    class InvalidEntries {

        @TempDir
        Path catalog;

        @Test
        void readsAValidEntry() throws IOException {
            write("sample.md", "---\n" + VALID_FRONTMATTER + "---\n\n## O que é\n");

            assertThat(repository().architectures()).singleElement().satisfies(sample -> {
                assertThat(sample.signature().annotations()).isEmpty();
                assertThat(sample.simplerAlternative()).isEmpty();
                assertThat(sample.markdown()).isEqualTo("## O que é");
            });
        }

        @Test
        void ignoresFilesThatAreNotMarkdown() throws IOException {
            write("sample.md", "---\n" + VALID_FRONTMATTER + "---\n");
            write("notes.txt", "não é uma arquitetura");

            assertThat(repository().architectures()).hasSize(1);
        }

        @Test
        void rejectsMissingDirectory() {
            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog.resolve("nope")).architectures())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.messageKey()).isEqualTo("catalog.error.read"));
        }

        @Test
        void rejectsFileWithoutFrontmatter() throws IOException {
            write("sample.md", "## Só markdown\n");

            assertError("catalog.error.missing-frontmatter", "sample.md");
        }

        @Test
        void rejectsUnclosedFrontmatter() throws IOException {
            write("sample.md", "---\n" + VALID_FRONTMATTER);

            assertError("catalog.error.missing-frontmatter", "sample.md");
        }

        @Test
        void rejectsInvalidYaml() throws IOException {
            write("sample.md", "---\nid: [sample\n---\n");

            assertThatThrownBy(() -> repository().architectures())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.messageKey()).isEqualTo("catalog.error.invalid-yaml"));
        }

        @Test
        void rejectsMissingRequiredField() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("nome: Sample\n", ""));

            assertError("catalog.error.missing-field", "sample.md", "nome");
        }

        @Test
        void namesTheFullPathOfNestedFields() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("  minimo_sinais: 2\n", ""));

            assertError("catalog.error.missing-field", "sample.md", "assinatura.minimo_sinais");
        }

        @Test
        void rejectsIdDifferentFromFileName() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("id: sample", "id: other"));

            assertError("catalog.error.id-mismatch", "sample.md", "other");
        }

        @Test
        void rejectsUnknownFamily() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("familia: simples", "familia: gigante"));

            assertError("catalog.error.invalid-value", "sample.md", "familia", "gigante");
        }

        @Test
        void rejectsUnknownComplexity() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("complexidade: baixa", "complexidade: extrema"));

            assertError("catalog.error.invalid-value", "sample.md", "custo.complexidade", "extrema");
        }

        @Test
        void rejectsMalformedClassRange() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("classes_por_feature: 4-6", "classes_por_feature: muitas"));

            assertError("catalog.error.invalid-value", "sample.md", "custo.classes_por_feature", "muitas");
        }

        @Test
        void rejectsInvertedClassRange() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("classes_por_feature: 4-6", "classes_por_feature: 6-4"));

            assertError("catalog.error.invalid-value", "sample.md", "custo.classes_por_feature", "6-4");
        }

        @Test
        void rejectsListFieldWithScalarValue() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("evolui_para: []", "evolui_para: layered"));

            assertError("catalog.error.invalid-value", "sample.md", "evolui_para", "layered");
        }

        @Test
        void rejectsIntegerFieldWithText() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("minimo_sinais: 2", "minimo_sinais: dois"));

            assertError("catalog.error.invalid-value", "sample.md", "assinatura.minimo_sinais", "dois");
        }

        @Test
        void namesTheIndexOfAnInvalidDependencyRule() throws IOException {
            writeSample(VALID_FRONTMATTER.replace("  - de: service\n", "  - para: service\n"));

            assertError("catalog.error.missing-field", "sample.md", "regras_dependencia[0].de");
        }

        private MarkdownCatalogRepository repository() {
            return new MarkdownCatalogRepository(catalog);
        }

        private void writeSample(String frontmatter) throws IOException {
            write("sample.md", "---\n" + frontmatter + "---\n");
        }

        private void write(String fileName, String content) throws IOException {
            Path architectures = Files.createDirectories(catalog.resolve("architectures"));
            Files.writeString(architectures.resolve(fileName), content);
        }

        private void assertError(String messageKey, Object... arguments) {
            assertThatThrownBy(() -> repository().architectures())
                    .isInstanceOfSatisfying(CatalogException.class, e -> {
                        assertThat(e.messageKey()).isEqualTo(messageKey);
                        assertThat(e.arguments()).containsExactly(arguments);
                    });
        }
    }
}
