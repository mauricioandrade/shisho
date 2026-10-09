package dev.mauricioandrade.shisho.infrastructure.cli;

import dev.mauricioandrade.shisho.domain.exception.ScaffoldException;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.CrudPlan;
import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.GeneratedFile;
import dev.mauricioandrade.shisho.domain.port.in.GenerateCrud;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.i18n.Messages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CrudCommandTest {

    private static final Messages MESSAGES = new Messages(Messages.DEFAULT_LOCALE);
    private static final List<ArchitectureProfile> ARCHITECTURES = MarkdownCatalogRepository.bundled().architectures();
    private static final GeneratedFile ENTITY = new GeneratedFile(Path.of("src/main/java/app/Usuario.java"), "class Usuario {}\n",
            "A entidade.");
    private static final GeneratedFile HANDLER = new GeneratedFile(Path.of("src/main/java/app/TratadorDeErros.java"), "x", "y");

    @TempDir
    Path project;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();
    private final List<CrudRequest> requests = new ArrayList<>();

    @Test
    void writesTheFilesAndExplainsEachOne() throws IOException {
        assertThat(run(plan(Optional.empty(), List.of()), project.toString(), "Usuario", "nome:String", "preco:BigDecimal")).isZero();

        assertThat(Files.readString(project.resolve(ENTITY.file()))).isEqualTo("class Usuario {}\n");
        assertThat(text(out)).isEqualTo("""
                師匠 Shishō — CRUD de Usuario em Pacotes por funcionalidade (Package by Feature)

                + src/main/java/app/Usuario.java
                  A entidade.

                = src/main/java/app/TratadorDeErros.java
                  Não foi criado: o projeto já tem uma classe que faz o papel deste arquivo.

                Criado 1 arquivo. Rode shisho analyze para conferir: o código gerado passa em todas as katas.
                """);
        assertThat(requests).singleElement().isEqualTo(new CrudRequest("Usuario",
                List.of(new CrudRequest.Field("nome", "String"), new CrudRequest.Field("preco", "BigDecimal")),
                Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    void passesTheChoicesOnAndWarnsAboutMixedStylesAndMissingDependencies() {
        CrudPlan plan = plan(Optional.of(architecture("layered")),
                List.of(List.of("spring-boot-starter-webmvc", "spring-boot-starter-web"), List.of("spring-boot-starter-data-jpa")));

        assertThat(run(plan, "--arquitetura", "package-by-feature", "--pacote", "com.loja", "--rota", "pessoas",
                project.toString(), "Usuario", "nome:String")).isZero();

        assertThat(requests).singleElement().satisfies(request -> {
            assertThat(request.architectureId()).contains("package-by-feature");
            assertThat(request.basePackage()).contains("com.loja");
            assertThat(request.route()).contains("pessoas");
        });
        assertThat(text(out)).contains("""
                ⚠ O projeto segue Camadas técnicas (Layered), e este CRUD segue Pacotes por funcionalidade (Package
                by Feature). Dois estilos no mesmo projeto obrigam quem lê a aprender os dois. Se a intenção é
                migrar, leve as features antigas junto.

                ⚠ O código gerado precisa destas dependências, que faltam no build:
                spring-boot-starter-webmvc/spring-boot-starter-web, spring-boot-starter-data-jpa.
                """);
    }

    @Test
    void neverOverwritesAndThenWritesNothing() throws IOException {
        Files.createDirectories(project.resolve(ENTITY.file()).getParent());
        Files.writeString(project.resolve(ENTITY.file()), "// meu código\n");
        GeneratedFile other = new GeneratedFile(Path.of("src/main/java/app/UsuarioService.java"), "class UsuarioService {}\n", "O service.");
        CrudPlan plan = new CrudPlan(architecture("layered"), List.of(other, ENTITY), List.of(), Optional.empty(), List.of(), Optional.empty());

        assertThat(run(plan, project.toString(), "Usuario", "nome:String")).isEqualTo(2);

        assertThat(Files.readString(project.resolve(ENTITY.file()))).isEqualTo("// meu código\n");
        assertThat(project.resolve(other.file())).doesNotExist();
        assertThat(text(err)).isEqualTo("""
                src/main/java/app/Usuario.java já existe.
                Nada foi gravado: o Shishō nunca sobrescreve um arquivo.
                """);
    }

    @Test
    void explainsTheFieldSyntax() {
        assertThat(run(plan(Optional.empty(), List.of()), project.toString(), "Usuario", "nome")).isEqualTo(2);

        assertThat(requests).isEmpty();
        assertThat(text(err)).isEqualTo("\"nome\" não é um campo: use nome:Tipo, como nome:String ou preco:BigDecimal.\n");
    }

    @Test
    void exitsWithTwoForAnInvalidRequest() {
        GenerateCrud refusing = (root, request) -> {
            throw new ScaffoldException("crud.error.no-architecture", "layered, package-by-feature");
        };

        assertThat(run(refusing, project.toString(), "Usuario", "nome:String")).isEqualTo(2);

        assertThat(text(err)).isEqualTo(
                "O Shishō não reconheceu a arquitetura do projeto. Escolha uma com --arquitetura: layered, package-by-feature.\n");
    }

    private int run(CrudPlan plan, String... arguments) {
        return run((root, request) -> {
            requests.add(request);
            return plan;
        }, arguments);
    }

    private int run(GenerateCrud generateCrud, String... arguments) {
        CrudCommand command = new CrudCommand(generateCrud, MESSAGES,
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        return new CommandLine(command).setResourceBundle(MESSAGES.bundle()).execute(arguments);
    }

    private CrudPlan plan(Optional<ArchitectureProfile> other, List<List<String>> missing) {
        return new CrudPlan(architecture("package-by-feature"), List.of(ENTITY), List.of(HANDLER), other, missing, Optional.empty());
    }

    private static ArchitectureProfile architecture(String id) {
        return ARCHITECTURES.stream().filter(profile -> profile.id().equals(id)).findFirst().orElseThrow();
    }

    private static String text(ByteArrayOutputStream stream) {
        return stream.toString(StandardCharsets.UTF_8).replace(System.lineSeparator(), "\n");
    }
}
