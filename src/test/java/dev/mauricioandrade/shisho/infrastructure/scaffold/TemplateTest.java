package dev.mauricioandrade.shisho.infrastructure.scaffold;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TemplateTest {

    private static final Map<String, List<Map<String, String>>> FIELDS = Map.of("campos", List.of(
            Map.of("campo", "nome", "tipo", "String"),
            Map.of("campo", "preco", "tipo", "BigDecimal")));

    @Test
    void fillsValues() {
        assertThat(Template.render("class {{Entidade}}Service {}", Map.of("Entidade", "Usuario"), Map.of()))
                .isEqualTo("class UsuarioService {}");
    }

    @Test
    void repeatsASectionInlineWithCommasBetweenItems() {
        assertThat(Template.render("{{Entidade}}({{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}})", Map.of("Entidade", "Produto"), FIELDS))
                .isEqualTo("Produto(String nome, BigDecimal preco)");
    }

    @Test
    void dropsTheLinesThatHoldOnlyASectionTag() {
        String template = """
                class Produto {
                {{#campos}}
                    private {{tipo}} {{campo}};
                {{/campos}}
                }
                """;

        assertThat(Template.render(template, Map.of(), FIELDS)).isEqualTo("""
                class Produto {
                    private String nome;
                    private BigDecimal preco;
                }
                """);
    }

    @Test
    void neverLeavesTrailingCommasSpacesOrRepeatedBlankLines() {
        String template = """
                import a.B;

                {{#importacoes}}
                import {{importacao}};
                {{/importacoes}}

                record R({{#campos}}
                        {{tipo}} {{campo}}{{,}}{{/campos}}) {
                }
                """;

        assertThat(Template.render(template, Map.of(), Map.of("importacoes", List.of(), "campos", FIELDS.get("campos"))))
                .isEqualTo("""
                        import a.B;

                        record R(
                                String nome,
                                BigDecimal preco) {
                        }
                        """);
    }

    @Test
    void leavesJavaBracesAlone() {
        assertThat(Template.render("@GetMapping(\"/{id}\")", Map.of(), Map.of())).isEqualTo("@GetMapping(\"/{id}\")");
    }

    @Test
    void rejectsWhatItWasNotGiven() {
        assertThatIllegalArgumentException().isThrownBy(() -> Template.render("{{pacote}}", Map.of(), Map.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> Template.render("{{#campos}}x{{/campos}}", Map.of(), Map.of()));
    }
}
