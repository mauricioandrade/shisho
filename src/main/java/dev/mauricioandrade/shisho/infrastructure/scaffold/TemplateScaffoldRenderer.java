package dev.mauricioandrade.shisho.infrastructure.scaffold;

import dev.mauricioandrade.shisho.domain.exception.ScaffoldException;
import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.GeneratedFile;
import dev.mauricioandrade.shisho.domain.model.Scaffold;
import dev.mauricioandrade.shisho.domain.port.out.ScaffoldRenderer;

import javax.lang.model.SourceVersion;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fills the catalog's templates for a Java project: the types a field may have, what each one
 * imports and how it is validated, and where Maven keeps the sources. See
 * docs/adr/0005-crud-scaffolding.md.
 */
public final class TemplateScaffoldRenderer implements ScaffoldRenderer {

    private static final Path SOURCES = Path.of("src", "main", "java");
    private static final Pattern ENTITY = Pattern.compile("\\p{Lu}[\\p{L}\\p{N}]*");
    private static final Pattern FIELD = Pattern.compile("\\p{Ll}[\\p{L}\\p{N}]*");
    private static final Pattern ROUTE = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
    private static final String TEXT_VALIDATION = "NotBlank";
    private static final String VALUE_VALIDATION = "NotNull";
    private static final String GENERATED_ID = "id";

    /** The types a field may have, in the order the error message lists them, and the import each one needs. */
    private static final Map<String, Optional<String>> TYPES = Collections.unmodifiableMap(types());

    private static Map<String, Optional<String>> types() {
        Map<String, Optional<String>> types = new LinkedHashMap<>();
        types.put("String", Optional.empty());
        types.put("Integer", Optional.empty());
        types.put("Long", Optional.empty());
        types.put("Double", Optional.empty());
        types.put("Boolean", Optional.empty());
        types.put("BigDecimal", Optional.of("java.math.BigDecimal"));
        types.put("LocalDate", Optional.of("java.time.LocalDate"));
        types.put("LocalDateTime", Optional.of("java.time.LocalDateTime"));
        types.put("Instant", Optional.of("java.time.Instant"));
        types.put("UUID", Optional.of("java.util.UUID"));
        return types;
    }

    @Override
    public List<GeneratedFile> render(Scaffold scaffold, String basePackage, CrudRequest request) {
        check(basePackage, request);
        Map<String, String> values = values(basePackage, request);
        Map<String, List<Map<String, String>>> lists = Map.of(
                "campos", request.fields().stream().map(TemplateScaffoldRenderer::field).toList(),
                "importacoes", request.fields().stream().flatMap(field -> TYPES.get(field.type()).stream())
                        .distinct().sorted().map(name -> Map.of("importacao", name)).toList(),
                "validacoes", request.fields().stream().map(field -> validation(field.type()))
                        .distinct().sorted().map(name -> Map.of("validacao", name)).toList());
        Path packageDirectory = SOURCES.resolve(basePackage.replace('.', '/'));
        return scaffold.files().stream()
                .map(file -> new GeneratedFile(
                        packageDirectory.resolve(Template.render(file.path(), values, lists)),
                        Template.render(file.content(), values, lists),
                        Template.render(file.explanation(), values, lists)))
                .toList();
    }

    private static void check(String basePackage, CrudRequest request) {
        if (!SourceVersion.isName(basePackage)) {
            throw new ScaffoldException("crud.error.package", basePackage);
        }
        if (!ENTITY.matcher(request.entity()).matches() || !SourceVersion.isName(request.entity())) {
            throw new ScaffoldException("crud.error.entity-name", request.entity());
        }
        if (request.fields().isEmpty()) {
            throw new ScaffoldException("crud.error.no-fields");
        }
        Set<String> names = new HashSet<>();
        for (CrudRequest.Field field : request.fields()) {
            if (!FIELD.matcher(field.name()).matches() || !SourceVersion.isName(field.name()) || field.name().equals(GENERATED_ID)) {
                throw new ScaffoldException("crud.error.field-name", field.name());
            }
            if (!names.add(field.name())) {
                throw new ScaffoldException("crud.error.duplicate-field", field.name());
            }
            if (!TYPES.containsKey(field.type())) {
                throw new ScaffoldException("crud.error.type", field.type(), String.join(", ", TYPES.keySet()));
            }
        }
        request.route().filter(route -> !ROUTE.matcher(route).matches()).ifPresent(route -> {
            throw new ScaffoldException("crud.error.route", route);
        });
    }

    private static Map<String, String> values(String basePackage, CrudRequest request) {
        String entity = request.entity();
        return Map.of(
                "pacote", basePackage,
                "Entidade", entity,
                "entidade", Character.toLowerCase(entity.charAt(0)) + entity.substring(1),
                "feature", entity.toLowerCase(Locale.ROOT),
                "rota", request.route().orElseGet(() -> plural(words(entity, "-"))),
                "tabela", plural(words(entity, "_")));
    }

    /** {@code ItemPedido} becomes {@code item-pedido} or {@code item_pedido}. */
    private static String words(String entity, String separator) {
        return entity.replaceAll("(?<=[\\p{Ll}\\p{N}])(?=\\p{Lu})", separator).toLowerCase(Locale.ROOT);
    }

    /** A naive plural, an s: {@code --rota} fixes the route when it is wrong. */
    private static String plural(String name) {
        return name.endsWith("s") ? name : name + "s";
    }

    private static Map<String, String> field(CrudRequest.Field field) {
        return Map.of(
                "campo", field.name(),
                "Campo", Character.toUpperCase(field.name().charAt(0)) + field.name().substring(1),
                "tipo", field.type(),
                "validacao", validation(field.type()));
    }

    private static String validation(String type) {
        return type.equals("String") ? TEXT_VALIDATION : VALUE_VALIDATION;
    }
}
