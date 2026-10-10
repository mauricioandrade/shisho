package dev.mauricioandrade.shisho.infrastructure.scaffold;

import dev.mauricioandrade.shisho.domain.exception.ScaffoldException;
import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.CrudRequest.Field;
import dev.mauricioandrade.shisho.domain.model.GeneratedFile;
import dev.mauricioandrade.shisho.domain.model.Scaffold;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TemplateScaffoldRendererTest {

    private static final Scaffold LAYERED = MarkdownCatalogRepository.bundled().scaffolds().stream()
            .filter(scaffold -> scaffold.architectureId().equals("layered")).findFirst().orElseThrow();

    private final TemplateScaffoldRenderer renderer = new TemplateScaffoldRenderer();

    @Test
    void placesEveryFileUnderTheBasePackage() {
        List<GeneratedFile> files = renderer.render(LAYERED, "com.loja", request("ItemPedido", "descricao:String"));

        assertThat(files).extracting(GeneratedFile::file).containsExactly(
                Path.of("src/main/java/com/loja/model/ItemPedido.java"),
                Path.of("src/main/java/com/loja/repository/ItemPedidoRepository.java"),
                Path.of("src/main/java/com/loja/dto/ItemPedidoRequest.java"),
                Path.of("src/main/java/com/loja/dto/ItemPedidoResponse.java"),
                Path.of("src/main/java/com/loja/service/ItemPedidoInexistenteException.java"),
                Path.of("src/main/java/com/loja/service/ItemPedidoService.java"),
                Path.of("src/main/java/com/loja/controller/ItemPedidoController.java"),
                Path.of("src/main/java/com/loja/controller/TratadorDeErros.java"));
        assertThat(files.get(6).content()).contains("@RequestMapping(\"/item-pedidos\")");
        assertThat(files.getFirst().explanation()).contains("quem cria um ItemPedido passa pelo construtor")
                .doesNotContain("{{").doesNotContain("\n");
    }

    @Test
    void knowsThePathsWithoutTheFields() {
        assertThat(renderer.paths(LAYERED, "com.loja", "ItemPedido"))
                .isEqualTo(renderer.render(LAYERED, "com.loja", request("ItemPedido", "descricao:String")).stream()
                        .map(GeneratedFile::file).toList());
        assertThat(renderer.paths(LAYERED, "com.loja", "itemPedido")).as("not an entity name").isEmpty();
    }

    @Test
    void writesTheFieldsTheirImportsAndTheirValidation() {
        List<GeneratedFile> files = renderer.render(LAYERED, "com.loja",
                request("Produto", "nome:String", "preco:BigDecimal", "lancamento:LocalDate"));

        assertThat(files.getFirst().content()).isEqualTo("""
                package com.loja.model;

                import jakarta.persistence.Entity;
                import jakarta.persistence.GeneratedValue;
                import jakarta.persistence.Id;
                import jakarta.persistence.Table;

                import java.math.BigDecimal;
                import java.time.LocalDate;

                @Entity
                @Table(name = "produtos")
                public class Produto {

                    @Id
                    @GeneratedValue
                    private Long id;
                    private String nome;
                    private BigDecimal preco;
                    private LocalDate lancamento;

                    protected Produto() {
                    }

                    public Produto(String nome, BigDecimal preco, LocalDate lancamento) {
                        this.nome = nome;
                        this.preco = preco;
                        this.lancamento = lancamento;
                    }

                    public void atualizar(String nome, BigDecimal preco, LocalDate lancamento) {
                        this.nome = nome;
                        this.preco = preco;
                        this.lancamento = lancamento;
                    }

                    public Long getId() {
                        return id;
                    }

                    public String getNome() {
                        return nome;
                    }

                    public BigDecimal getPreco() {
                        return preco;
                    }

                    public LocalDate getLancamento() {
                        return lancamento;
                    }
                }
                """);
        assertThat(files.get(2).content()).isEqualTo("""
                package com.loja.dto;

                import jakarta.validation.constraints.NotBlank;
                import jakarta.validation.constraints.NotNull;

                import java.math.BigDecimal;
                import java.time.LocalDate;

                public record ProdutoRequest(
                        @NotBlank String nome,
                        @NotNull BigDecimal preco,
                        @NotNull LocalDate lancamento) {
                }
                """);
    }

    @Test
    void takesTheRouteGiven() {
        CrudRequest request = new CrudRequest("ItemPedido", List.of(new Field("nome", "String")), Optional.empty(), Optional.empty(),
                Optional.of("itens-pedido"));

        assertThat(renderer.render(LAYERED, "com.loja", request).get(6).content()).contains("@RequestMapping(\"/itens-pedido\")");
    }

    @Test
    void rejectsWhatWouldNotCompile() {
        assertInvalid("com.loja", request("usuario", "nome:String"), "crud.error.entity-name", "usuario");
        assertInvalid("com.loja", request("Item-Pedido", "nome:String"), "crud.error.entity-name", "Item-Pedido");
        assertInvalid("com.loja", request("Usuario"), "crud.error.no-fields");
        assertInvalid("com.loja", request("Usuario", "id:Long"), "crud.error.field-name", "id");
        assertInvalid("com.loja", request("Usuario", "default:String"), "crud.error.field-name", "default");
        assertInvalid("com.loja", request("Usuario", "nome:String", "nome:String"), "crud.error.duplicate-field", "nome");
        assertInvalid("com.loja", request("Usuario", "foto:byte[]"), "crud.error.type", "byte[]",
                "String, Integer, Long, Double, Boolean, BigDecimal, LocalDate, LocalDateTime, Instant, UUID");
        assertInvalid("com.1loja", request("Usuario", "nome:String"), "crud.error.package", "com.1loja");
    }

    private void assertInvalid(String basePackage, CrudRequest request, String key, Object... arguments) {
        assertThatExceptionOfType(ScaffoldException.class).isThrownBy(() -> renderer.render(LAYERED, basePackage, request))
                .satisfies(e -> assertThat(e.messageKey()).isEqualTo(key))
                .satisfies(e -> assertThat(e.arguments()).containsExactly(arguments));
    }

    private static CrudRequest request(String entity, String... fields) {
        List<Field> parsed = Arrays.stream(fields).map(field -> field.split(":")).map(parts -> new Field(parts[0], parts[1])).toList();
        return new CrudRequest(entity, parsed, Optional.empty(), Optional.empty(), Optional.empty());
    }
}
