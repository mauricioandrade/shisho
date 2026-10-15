package dev.mauricioandrade.shisho;

import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Shishō judges what it generates: every catalog template, applied to a project like the
 * Spring Initializr's or code.quarkus.io's, must be recognized as its architecture and teach no
 * lesson at all.
 */
class CrudOracleTest {

    private static final Path INITIALIZR = Path.of("src/test/resources/fixtures/initializr");
    /** No class shows the base package, so the first CRUD is told it. */
    private static final Path CODE_QUARKUS = Path.of("src/test/resources/fixtures/code-quarkus");

    /** What the report says in each language: the architecture line, the clean rank and the crud header. */
    private record Language(String code, Optional<String> translation, String architecture, String cleanRank, String crudHeader) {

        String name(String architectureId) {
            return MarkdownCatalogRepository.bundled(translation).architectures().stream()
                    .filter(profile -> profile.id().equals(architectureId))
                    .map(ArchitectureProfile::name).findFirst().orElseThrow();
        }

        @Override
        public String toString() {
            return code;
        }
    }

    private static final Language PORTUGUESE = new Language("pt", Optional.empty(),
            "\nArquitetura: ", "Graduação: 1º dan  (nenhuma lição)\n", "師匠 Shishō — CRUD de %s em %s\n");
    private static final Language ENGLISH = new Language("en", Optional.of("en"),
            "\nArchitecture: ", "Rank: 1st dan  (no lessons)\n", "師匠 Shishō — CRUD of %s in %s\n");

    /** A starting project of the template's stack. */
    private record Stack(String name, Path fixture) {

        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Arguments> scaffolds() {
        return Stream.of(PORTUGUESE, ENGLISH).flatMap(language -> MarkdownCatalogRepository.bundled().scaffolds().stream()
                .map(scaffold -> Arguments.of(language, scaffold.architectureId(), scaffold.buildGroup().isPresent()
                        ? new Stack("quarkus", CODE_QUARKUS)
                        : new Stack("spring", INITIALIZR))));
    }

    @ParameterizedTest(name = "{1}, {2} ({0})")
    @MethodSource("scaffolds")
    void theGeneratedCrudPassesEveryKata(Language language, String architectureId, Stack stack, @TempDir Path project)
            throws IOException {
        copy(stack.fixture(), project);

        assertThat(run(language, "crud", "--arquitetura", architectureId, "--mesmo-assim", "--pacote", "com.exemplo.loja",
                project.toString(), "Produto",
                "nome:String", "estoque:Integer", "codigoBarras:Long", "peso:Double", "ativo:Boolean", "preco:BigDecimal",
                "lancamento:LocalDate", "atualizadoEm:LocalDateTime", "criadoEm:Instant", "codigo:UUID")).isZero();
        assertThat(run(language, "crud", "--arquitetura", architectureId, project.toString(), "ItemPedido", "quantidade:Integer")).isZero();

        String report = text(run(language, "analyze", project.toString()), "analyze");
        assertThat(report)
                .contains(language.architecture() + language.name(architectureId) + "  (")
                .endsWith(language.cleanRank());
    }

    @ParameterizedTest(name = "{1}, {2} ({0})")
    @MethodSource("scaffolds")
    void theSecondCrudFollowsTheArchitectureWithoutBeingTold(Language language, String architectureId, Stack stack,
                                                             @TempDir Path project) throws IOException {
        copy(stack.fixture(), project);
        run(language, "crud", "--arquitetura", architectureId, "--mesmo-assim", "--pacote", "com.exemplo.loja", project.toString(),
                "Produto", "nome:String");

        assertThat(run(language, "crud", project.toString(), "Fornecedor", "nome:String")).isZero();
        assertThat(lastOut).startsWith(language.crudHeader().formatted("Fornecedor", language.name(architectureId)));
    }

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private String lastOut = "";

    private int run(Language language, String... arguments) {
        out.reset();
        err.reset();
        int exitCode = ShishoBootstrap.run(arguments, language.code(), out, err);
        lastOut = out.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
        assertThat(err.toString(StandardCharsets.UTF_8)).as(String.join(" ", arguments)).isEmpty();
        return exitCode;
    }

    private String text(int exitCode, String command) {
        assertThat(exitCode).as(command).isZero();
        return lastOut;
    }

    private static void copy(Path fixture, Path project) throws IOException {
        try (Stream<Path> files = Files.walk(fixture)) {
            for (Path file : files.toList()) {
                Path target = project.resolve(fixture.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(file, target);
                }
            }
        }
    }
}
