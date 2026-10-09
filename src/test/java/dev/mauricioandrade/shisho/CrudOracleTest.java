package dev.mauricioandrade.shisho;

import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.Scaffold;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Shishō judges what it generates: every catalog template, applied to a project like the
 * Spring Initializr's, must be recognized as its architecture and teach no lesson at all.
 */
class CrudOracleTest {

    private static final MarkdownCatalogRepository CATALOG = MarkdownCatalogRepository.bundled();
    private static final Path INITIALIZR = Path.of("src/test/resources/fixtures/initializr");

    static Stream<String> scaffolds() {
        return CATALOG.scaffolds().stream().map(Scaffold::architectureId);
    }

    @ParameterizedTest
    @MethodSource("scaffolds")
    void theGeneratedCrudPassesEveryKata(String architectureId, @TempDir Path project) throws IOException {
        copyInitializr(project);

        assertThat(run("crud", "--arquitetura", architectureId, project.toString(), "Produto",
                "nome:String", "estoque:Integer", "codigoBarras:Long", "peso:Double", "ativo:Boolean", "preco:BigDecimal",
                "lancamento:LocalDate", "atualizadoEm:LocalDateTime", "criadoEm:Instant", "codigo:UUID")).isZero();
        assertThat(run("crud", "--arquitetura", architectureId, project.toString(), "ItemPedido", "quantidade:Integer")).isZero();

        String report = text(run("analyze", project.toString()), "analyze");
        assertThat(report)
                .contains("\nArquitetura: " + name(architectureId) + "  (")
                .endsWith("Graduação: 1º dan  (nenhuma lição)\n");
    }

    @ParameterizedTest
    @MethodSource("scaffolds")
    void theNextCrudFollowsTheArchitectureWithoutBeingTold(String architectureId, @TempDir Path project) throws IOException {
        copyInitializr(project);
        run("crud", "--arquitetura", architectureId, project.toString(), "Produto", "nome:String");
        run("crud", "--arquitetura", architectureId, project.toString(), "Cliente", "nome:String");

        assertThat(run("crud", project.toString(), "Fornecedor", "nome:String")).isZero();
        assertThat(lastOut).startsWith("師匠 Shishō — CRUD de Fornecedor em " + name(architectureId) + "\n");
    }

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private String lastOut = "";

    private int run(String... arguments) {
        out.reset();
        err.reset();
        int exitCode = ShishoBootstrap.run(arguments, out, err);
        lastOut = out.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
        assertThat(err.toString(StandardCharsets.UTF_8)).as(String.join(" ", arguments)).isEmpty();
        return exitCode;
    }

    private String text(int exitCode, String command) {
        assertThat(exitCode).as(command).isZero();
        return lastOut;
    }

    private static String name(String architectureId) {
        return CATALOG.architectures().stream().filter(profile -> profile.id().equals(architectureId))
                .map(ArchitectureProfile::name).findFirst().orElseThrow();
    }

    private static void copyInitializr(Path project) throws IOException {
        try (Stream<Path> files = Files.walk(INITIALIZR)) {
            for (Path file : files.toList()) {
                Path target = project.resolve(INITIALIZR.relativize(file).toString());
                if (Files.isDirectory(file)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(file, target);
                }
            }
        }
    }
}
