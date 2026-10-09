package dev.mauricioandrade.shisho.infrastructure.catalog;

import dev.mauricioandrade.shisho.domain.exception.CatalogException;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.DependencyRule;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.KataExample;
import dev.mauricioandrade.shisho.domain.model.KataExample.Snippet;
import dev.mauricioandrade.shisho.domain.model.ProjectContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
            referencias: [https://example.com]
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
            assertThat(hexagonal.signature().packages()).contains("port/in", "adapter/out");
            assertThat(hexagonal.signature().annotations()).isEmpty();
            assertThat(hexagonal.signature().minimumSignals()).isEqualTo(2);
            assertThat(hexagonal.dependencyRules()).containsExactly(
                    new DependencyRule("domain", List.of("adapter", "infrastructure", "org.springframework", "jakarta.persistence")),
                    new DependencyRule("application", List.of("adapter")));
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
    class KataExamples {

        @TempDir
        Path catalog;

        @Test
        void readsTheCodeBeforeAndAfterWithTheirExplanation() throws IOException {
            write("system-out.md", """
                    ---
                    id: system-out
                    ---

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

            assertThat(new MarkdownCatalogRepository(catalog).kataExamples()).containsExactly(new KataExample("system-out",
                    new Snippet("java", "System.out.println(\"x\");", ""),
                    new Snippet("java", "log.info(\"x\");\n\nlog.debug(\"y\");", "O logger tem nível e formato.")));
        }

        @Test
        void rejectsAnExampleWithoutBothSectionsOrCode() throws IOException {
            write("system-out.md", "---\nid: system-out\n---\n\n## Antes\n\n```java\nx();\n```\n");
            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).kataExamples())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.arguments()).containsExactly("system-out.md", "## Depois"));

            write("system-out.md", "---\nid: system-out\n---\n\n## Antes\n\nsó texto\n\n## Depois\n\n```java\nx();\n```\n");
            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).kataExamples())
                    .isInstanceOfSatisfying(CatalogException.class, e -> {
                        assertThat(e.messageKey()).isEqualTo("catalog.error.missing-code");
                        assertThat(e.arguments()).containsExactly("system-out.md", "Antes");
                    });
        }

        @Test
        void rejectsAnIdDifferentFromTheFileName() throws IOException {
            write("system-out.md", "---\nid: sistema\n---\n");

            assertThatThrownBy(() -> new MarkdownCatalogRepository(catalog).kataExamples())
                    .isInstanceOfSatisfying(CatalogException.class, e ->
                            assertThat(e.messageKey()).isEqualTo("catalog.error.id-mismatch"));
        }

        private void write(String fileName, String content) throws IOException {
            Files.writeString(Files.createDirectories(catalog.resolve("katas")).resolve(fileName), content);
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
