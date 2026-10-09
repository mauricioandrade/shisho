package dev.mauricioandrade.shisho.infrastructure.catalog;

import dev.mauricioandrade.shisho.domain.exception.CatalogException;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.DependencyRule;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.KataExample;
import dev.mauricioandrade.shisho.domain.model.KataExample.Snippet;
import dev.mauricioandrade.shisho.domain.port.out.CatalogRepository;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads {@code catalog/architectures/*.md} and {@code catalog/katas/*.md}: YAML frontmatter
 * between {@code ---} lines, followed by Markdown. The schema is documented in
 * {@code catalog/README.md}.
 */
public final class MarkdownCatalogRepository implements CatalogRepository {

    private static final String FRONTMATTER_DELIMITER = "---";
    private static final String EXTENSION = ".md";
    private static final Pattern CLASS_RANGE = Pattern.compile("(\\d+)-(\\d+)");
    private static final String BUNDLED_CATALOG = "/catalog";

    private static final String BEFORE = "Antes";
    private static final String AFTER = "Depois";
    private static final Pattern SECTION = Pattern.compile("(?m)^## (.+)$");
    private static final Pattern CODE_BLOCK = Pattern.compile("(?ms)^```(\\w+)\\n(.*?)^```$");

    private final Path catalogRoot;

    public MarkdownCatalogRepository(Path catalogRoot) {
        this.catalogRoot = catalogRoot;
    }

    /** The catalog packaged with the Shishō: a directory when run from the build, a folder of the jar otherwise. */
    public static MarkdownCatalogRepository bundled() {
        URL catalog = MarkdownCatalogRepository.class.getResource(BUNDLED_CATALOG);
        if (catalog == null) {
            throw new CatalogException("catalog.error.read", BUNDLED_CATALOG);
        }
        try {
            URI uri = catalog.toURI();
            if (!uri.getScheme().equals("jar")) {
                return new MarkdownCatalogRepository(Path.of(uri));
            }
            return new MarkdownCatalogRepository(jarFileSystem(uri).getPath(BUNDLED_CATALOG));
        } catch (URISyntaxException | IOException e) {
            throw new CatalogException("catalog.error.read", e, catalog.toString());
        }
    }

    /** Opened once and kept open for the life of the process, like the jar itself. */
    private static FileSystem jarFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.newFileSystem(uri, Map.of());
        } catch (FileSystemAlreadyExistsException e) {
            return FileSystems.getFileSystem(uri);
        }
    }

    @Override
    public List<ArchitectureProfile> architectures() {
        return documents("architectures").stream()
                .map(document -> toProfile(document.fileName(), document.fields(), document.markdown()))
                .toList();
    }

    @Override
    public List<KataExample> kataExamples() {
        return documents("katas").stream().map(MarkdownCatalogRepository::toExample).toList();
    }

    private record Document(String fileName, FrontmatterFields fields, String markdown) {

        /** The {@code id} field, which must be the file name without the extension. */
        String id() {
            String id = fields.text("id");
            if (!id.equals(fileName.substring(0, fileName.length() - EXTENSION.length()))) {
                throw new CatalogException("catalog.error.id-mismatch", fileName, id);
            }
            return id;
        }
    }

    private List<Document> documents(String folder) {
        Path directory = catalogRoot.resolve(folder);
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(file -> file.getFileName().toString().endsWith(EXTENSION))
                    .sorted()
                    .map(MarkdownCatalogRepository::read)
                    .toList();
        } catch (IOException e) {
            throw new CatalogException("catalog.error.read", e, directory.toString());
        }
    }

    private static Document read(Path file) {
        String fileName = file.getFileName().toString();
        String content;
        try {
            content = Files.readString(file);
        } catch (IOException e) {
            throw new CatalogException("catalog.error.read", e, file.toString());
        }
        List<String> lines = content.lines().toList();
        if (lines.isEmpty() || !lines.getFirst().equals(FRONTMATTER_DELIMITER)) {
            throw new CatalogException("catalog.error.missing-frontmatter", fileName);
        }
        int closing = lines.subList(1, lines.size()).indexOf(FRONTMATTER_DELIMITER) + 1;
        if (closing == 0) {
            throw new CatalogException("catalog.error.missing-frontmatter", fileName);
        }
        String yaml = String.join("\n", lines.subList(1, closing));
        String markdown = String.join("\n", lines.subList(closing + 1, lines.size())).strip();
        return new Document(fileName, parseYaml(fileName, yaml), markdown);
    }

    private static KataExample toExample(Document document) {
        String id = document.id();
        Map<String, String> sections = new LinkedHashMap<>();
        Matcher heading = SECTION.matcher(document.markdown());
        List<MatchResult> headings = heading.results().toList();
        for (int i = 0; i < headings.size(); i++) {
            int end = i + 1 < headings.size() ? headings.get(i + 1).start() : document.markdown().length();
            sections.put(headings.get(i).group(1).strip(), document.markdown().substring(headings.get(i).end(), end));
        }
        return new KataExample(id, snippet(document.fileName(), sections, BEFORE), snippet(document.fileName(), sections, AFTER));
    }

    private static Snippet snippet(String fileName, Map<String, String> sections, String section) {
        String text = sections.get(section);
        if (text == null) {
            throw new CatalogException("catalog.error.missing-field", fileName, "## " + section);
        }
        Matcher code = CODE_BLOCK.matcher(text);
        if (!code.find()) {
            throw new CatalogException("catalog.error.missing-code", fileName, section);
        }
        String explanation = (text.substring(0, code.start()) + text.substring(code.end())).strip().replaceAll("\\s*\\n\\s*", " ");
        return new Snippet(code.group(1), code.group(2).stripTrailing(), explanation);
    }

    private static FrontmatterFields parseYaml(String fileName, String yaml) {
        Object document;
        try {
            document = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
        } catch (YAMLException e) {
            throw new CatalogException("catalog.error.invalid-yaml", e, fileName, e.getMessage());
        }
        if (!(document instanceof Map<?, ?> values)) {
            throw new CatalogException("catalog.error.missing-frontmatter", fileName);
        }
        return new FrontmatterFields(fileName, values);
    }

    private static ArchitectureProfile toProfile(String fileName, FrontmatterFields fields, String markdown) {
        String id = new Document(fileName, fields, markdown).id();
        FrontmatterFields signature = fields.section("assinatura");
        FrontmatterFields cost = fields.section("custo");
        return new ArchitectureProfile(
                id,
                fields.text("nome"),
                family(fields),
                new Signature(
                        signature.textList("pacotes"),
                        signature.optionalTextList("anotacoes"),
                        signature.integer("minimo_sinais")),
                fields.sectionList("regras_dependencia").stream()
                        .map(rule -> new DependencyRule(rule.text("de"), rule.textList("nao_pode_depender_de")))
                        .toList(),
                new Cost(classRange(cost), complexity(cost)),
                fields.textList("faz_sentido_quando"),
                fields.textList("sinais_overengineering"),
                fields.textList("sinais_subengenharia"),
                fields.textList("evolui_para"),
                fields.optionalText("alternativa_mais_simples"),
                fields.textList("referencias"),
                markdown);
    }

    private static Family family(FrontmatterFields fields) {
        String value = fields.text("familia");
        return switch (value) {
            case "simples" -> Family.SIMPLE;
            case "modular" -> Family.MODULAR;
            case "isolamento-de-dominio" -> Family.DOMAIN_ISOLATION;
            case "distribuida" -> Family.DISTRIBUTED;
            default -> throw fields.invalid("familia", value);
        };
    }

    private static Complexity complexity(FrontmatterFields cost) {
        String value = cost.text("complexidade");
        return switch (value) {
            case "baixa" -> Complexity.LOW;
            case "media" -> Complexity.MEDIUM;
            case "alta" -> Complexity.HIGH;
            default -> throw cost.invalid("complexidade", value);
        };
    }

    private static ClassRange classRange(FrontmatterFields cost) {
        String value = cost.text("classes_por_feature");
        Matcher matcher = CLASS_RANGE.matcher(value);
        if (!matcher.matches()) {
            throw cost.invalid("classes_por_feature", value);
        }
        int min = Integer.parseInt(matcher.group(1));
        int max = Integer.parseInt(matcher.group(2));
        if (min > max) {
            throw cost.invalid("classes_por_feature", value);
        }
        return new ClassRange(min, max);
    }
}
