package dev.mauricioandrade.shisho.infrastructure.catalog;

import dev.mauricioandrade.shisho.domain.exception.CatalogException;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.DependencyRule;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.AvoidedDependency;
import dev.mauricioandrade.shisho.domain.model.Concept;
import dev.mauricioandrade.shisho.domain.model.Concept.Group;
import dev.mauricioandrade.shisho.domain.model.Concept.Signal;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaFeature;
import dev.mauricioandrade.shisho.domain.model.JavaFeature.Status;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Example;
import dev.mauricioandrade.shisho.domain.model.KataGuide.Snippet;
import dev.mauricioandrade.shisho.domain.model.Scaffold;
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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads the catalog's folders ({@code architectures}, {@code katas}, {@code java-features},
 * {@code dependencies}, {@code scaffolds}): YAML frontmatter between {@code ---} lines, followed by Markdown. The schema is documented in
 * {@code catalog/README.md}.
 */
public final class MarkdownCatalogRepository implements CatalogRepository {

    private static final String FRONTMATTER_DELIMITER = "---";
    private static final String EXTENSION = ".md";
    private static final Pattern CLASS_RANGE = Pattern.compile("(\\d+)-(\\d+)");
    private static final Pattern COORDINATES = Pattern.compile("([^:\\s]+):([^:\\s]+)");
    private static final String BUNDLED_CATALOG = "/catalog";

    /** A kata guide's sections, by their Portuguese and English headings. */
    private static final List<String> WHY = List.of("Por quê", "Why");
    private static final List<String> ACCEPTABLE_WHEN = List.of("Quando é aceitável", "Acceptable when");
    private static final List<String> BEFORE = List.of("Antes", "Before");
    private static final List<String> AFTER = List.of("Depois", "After");
    /** {@code ## Antes (Quarkus)}: the before of a variant, whose after is {@code ## Depois (Quarkus)}. */
    private static final Pattern VARIANT_BEFORE = Pattern.compile("(?:Antes|Before) \\((.+)\\)");
    private static final List<String> WHAT_IT_IS = List.of("O que é", "What it is");
    private static final List<String> EXAMPLE = List.of("Exemplo", "Example");
    /** The conditions a concept signal may set. See docs/adr/0009-shoshin.md. */
    private static final List<String> SIGNAL_KEYS =
            List.of("anotacao", "tipo", "modificador", "import", "estende", "chamada", "sufixo", "detector");
    private static final Set<String> KINDS = Set.of("class", "interface", "enum", "record", "annotation");
    /** What a translation may change: the text, never the data. See docs/adr/0007-languages.md. */
    private static final Set<String> TRANSLATABLE = Set.of("id", "nome", "resumo", "alternativa", "uma_vez_por_projeto");
    private static final Pattern SECTION = Pattern.compile("(?m)^## (.+)$");
    private static final Pattern CODE_BLOCK = Pattern.compile("(?ms)^```(\\w+)\\n(.*?)^```$");
    private static final Pattern BLANK_LINES = Pattern.compile("\\n\\s*\\n");

    private final Path catalogRoot;
    private final Optional<String> translation;

    /** The catalog in Portuguese, its source language. */
    public MarkdownCatalogRepository(Path catalogRoot) {
        this(catalogRoot, Optional.empty());
    }

    /** {@code translation}: the folder under the catalog root with the translated text, such as {@code en}. */
    public MarkdownCatalogRepository(Path catalogRoot, Optional<String> translation) {
        this.catalogRoot = catalogRoot;
        this.translation = translation;
    }

    /** The catalog packaged with the Shishō: a directory when run from the build, a folder of the jar otherwise. */
    public static MarkdownCatalogRepository bundled() {
        return bundled(Optional.empty());
    }

    /** The packaged catalog, with the text of {@code translation} (such as {@code en}) where it has one. */
    public static MarkdownCatalogRepository bundled(Optional<String> translation) {
        URL catalog = MarkdownCatalogRepository.class.getResource(BUNDLED_CATALOG);
        if (catalog == null) {
            throw new CatalogException("catalog.error.read", BUNDLED_CATALOG);
        }
        try {
            URI uri = catalog.toURI();
            if (!uri.getScheme().equals("jar")) {
                return new MarkdownCatalogRepository(Path.of(uri), translation);
            }
            return new MarkdownCatalogRepository(jarFileSystem(uri).getPath(BUNDLED_CATALOG), translation);
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
    public List<KataGuide> kataGuides() {
        return documents("katas").stream().map(MarkdownCatalogRepository::toGuide).toList();
    }

    @Override
    public List<JavaFeature> javaFeatures() {
        return documents("java-features").stream().map(MarkdownCatalogRepository::toFeature).toList();
    }

    @Override
    public List<AvoidedDependency> avoidedDependencies() {
        return documents("dependencies").stream().map(MarkdownCatalogRepository::toAvoidedDependency).toList();
    }

    @Override
    public List<Scaffold> scaffolds() {
        return documents("scaffolds").stream().map(MarkdownCatalogRepository::toScaffold).toList();
    }

    @Override
    public List<Concept> concepts() {
        return documents("concepts").stream()
                .map(MarkdownCatalogRepository::toConcept)
                .sorted(Comparator.comparing(Concept::group).thenComparingInt(Concept::order).thenComparing(Concept::id))
                .toList();
    }

    private static Concept toConcept(Document document) {
        FrontmatterFields fields = document.fields();
        List<Signal> signals = fields.sectionList("aparece_em").stream().map(MarkdownCatalogRepository::signal).toList();
        if (signals.isEmpty()) {
            throw fields.invalid("aparece_em", signals);
        }
        List<String> references = fields.textList("referencias");
        if (references.isEmpty()) {
            throw fields.invalid("referencias", references);
        }
        Map<String, String> sections = sections(document.markdown());
        return new Concept(document.id(), fields.text("nome"), group(fields), fields.integer("ordem"), signals, references,
                paragraphs(document.fileName(), sections, heading(sections, WHAT_IT_IS)),
                snippet(document.fileName(), sections, heading(sections, EXAMPLE)));
    }

    /** An unknown key is an error: a misspelled condition would match every type. */
    private static Signal signal(FrontmatterFields fields) {
        List<String> keys = fields.keys();
        keys.stream().filter(key -> !SIGNAL_KEYS.contains(key)).findFirst().ifPresent(key -> {
            throw fields.invalid(key, String.join(", ", SIGNAL_KEYS));
        });
        if (keys.isEmpty() || keys.contains("detector") && keys.size() > 1) {
            throw fields.invalid("detector", keys);
        }
        Optional<String> kind = fields.optionalText("tipo");
        kind.filter(value -> !KINDS.contains(value)).ifPresent(value -> {
            throw fields.invalid("tipo", value);
        });
        return new Signal(fields.optionalText("anotacao").map(name -> name.startsWith("@") ? name.substring(1) : name),
                kind, fields.optionalText("modificador"), fields.optionalText("import"), fields.optionalText("estende"),
                fields.optionalText("chamada"), fields.optionalText("sufixo"), fields.optionalText("detector"));
    }

    private static Group group(FrontmatterFields fields) {
        String value = fields.text("grupo");
        return switch (value) {
            case "java" -> Group.JAVA;
            case "spring" -> Group.SPRING;
            case "quarkus" -> Group.QUARKUS;
            case "jakarta" -> Group.JAKARTA;
            case "padrao" -> Group.PATTERN;
            default -> throw fields.invalid("grupo", value);
        };
    }

    /** Every {@code ## path} section is a file: the prose before its code block explains it. */
    private static Scaffold toScaffold(Document document) {
        FrontmatterFields fields = document.fields();
        Map<String, String> once = new LinkedHashMap<>(fields.optionalTextMap("uma_vez_por_projeto"));
        Map<String, String> sections = sections(document.markdown());
        if (sections.isEmpty()) {
            throw new CatalogException("catalog.error.missing-field", document.fileName(), "## <arquivo>");
        }
        List<Scaffold.File> files = sections.keySet().stream().map(path -> {
            Snippet snippet = snippet(document.fileName(), sections, path);
            return new Scaffold.File(path, snippet.explanation(), snippet.code() + "\n", Optional.ofNullable(once.remove(path)));
        }).toList();
        if (!once.isEmpty()) {
            throw fields.invalid("uma_vez_por_projeto", once.keySet().iterator().next());
        }
        List<List<String>> dependencies = fields.textList("dependencias").stream()
                .map(alternatives -> List.of(alternatives.split("\\|")))
                .toList();
        return new Scaffold(fields.optionalText("arquitetura").orElse(document.id()), fields.optionalText("grupo_do_build"),
                dependencies, fields.optionalTextList("recusa_imports"), files);
    }

    private static AvoidedDependency toAvoidedDependency(Document document) {
        FrontmatterFields fields = document.fields();
        List<Dependency> coordinates = fields.textList("coordenadas").stream()
                .map(value -> coordinates(fields, value))
                .toList();
        if (coordinates.isEmpty()) {
            throw fields.invalid("coordenadas", coordinates);
        }
        List<String> references = fields.textList("referencias");
        if (references.isEmpty()) {
            throw fields.invalid("referencias", references);
        }
        return new AvoidedDependency(document.id(), fields.text("nome"), coordinates, fields.text("resumo"),
                fields.text("alternativa"), references, document.markdown());
    }

    /** {@code groupId:artifactId}. */
    private static Dependency coordinates(FrontmatterFields fields, String value) {
        Matcher matcher = COORDINATES.matcher(value);
        if (!matcher.matches()) {
            throw fields.invalid("coordenadas", value);
        }
        return new Dependency(matcher.group(1), matcher.group(2));
    }

    private static JavaFeature toFeature(Document document) {
        FrontmatterFields fields = document.fields();
        return new JavaFeature(document.id(), fields.text("nome"), new JavaVersion(fields.integer("desde")),
                status(fields), fields.integer("jep"), document.markdown());
    }

    private static Status status(FrontmatterFields fields) {
        String value = fields.text("status");
        return switch (value) {
            case "final" -> Status.FINAL;
            case "preview" -> Status.PREVIEW;
            default -> throw fields.invalid("status", value);
        };
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
        List<Document> originals = markdownFiles(catalogRoot.resolve(folder)).stream().map(MarkdownCatalogRepository::read).toList();
        return translation
                .map(language -> catalogRoot.resolve(language).resolve(folder))
                .map(directory -> originals.stream().map(original -> translated(original, directory.resolve(original.fileName()))).toList())
                .orElse(originals);
    }

    private static List<Path> markdownFiles(Path directory) {
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(file -> file.getFileName().toString().endsWith(EXTENSION)).sorted().toList();
        } catch (IOException e) {
            throw new CatalogException("catalog.error.read", e, directory.toString());
        }
    }

    /** Without a translation, the original: Portuguese beats no explanation at all. */
    private static Document translated(Document original, Path file) {
        if (!Files.isRegularFile(file)) {
            return original;
        }
        Document translation = read(file);
        translation.id();
        return new Document(original.fileName(), original.fields().translatedBy(translation.fields(), TRANSLATABLE),
                translation.markdown());
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

    private static KataGuide toGuide(Document document) {
        String id = document.id();
        Map<String, String> sections = sections(document.markdown());
        List<String> references = document.fields().textList("referencias");
        if (references.isEmpty()) {
            throw document.fields().invalid("referencias", references);
        }
        return new KataGuide(id,
                paragraphs(document.fileName(), sections, heading(sections, WHY)),
                paragraphs(document.fileName(), sections, heading(sections, ACCEPTABLE_WHEN)),
                snippet(document.fileName(), sections, heading(sections, BEFORE)),
                snippet(document.fileName(), sections, heading(sections, AFTER)),
                variants(document.fileName(), sections),
                references);
    }

    /** By the variant's name in lower case: {@code ## Antes (Quarkus)} is the variant {@code quarkus}. */
    private static Map<String, Example> variants(String fileName, Map<String, String> sections) {
        Map<String, Example> variants = new LinkedHashMap<>();
        for (String section : sections.keySet()) {
            Matcher before = VARIANT_BEFORE.matcher(section);
            if (before.matches()) {
                String after = (section.startsWith("Antes") ? "Depois" : "After") + " (" + before.group(1) + ")";
                variants.put(before.group(1).toLowerCase(Locale.ROOT),
                        new Example(snippet(fileName, sections, section), snippet(fileName, sections, after)));
            }
        }
        return variants;
    }

    /** The text under each {@code ## heading}, by heading, in order. */
    private static Map<String, String> sections(String markdown) {
        Map<String, String> sections = new LinkedHashMap<>();
        List<MatchResult> headings = SECTION.matcher(markdown).results().toList();
        for (int i = 0; i < headings.size(); i++) {
            int end = i + 1 < headings.size() ? headings.get(i + 1).start() : markdown.length();
            sections.put(headings.get(i).group(1).strip(), markdown.substring(headings.get(i).end(), end));
        }
        return sections;
    }

    /** The heading the document uses, in either language; the Portuguese one when it has neither. */
    private static String heading(Map<String, String> sections, List<String> headings) {
        return headings.stream().filter(sections::containsKey).findFirst().orElse(headings.getFirst());
    }

    /** The section's prose, one string per paragraph, with its line breaks turned into spaces. */
    private static List<String> paragraphs(String fileName, Map<String, String> sections, String section) {
        String text = sections.getOrDefault(section, "").strip();
        if (text.isEmpty()) {
            throw new CatalogException("catalog.error.missing-field", fileName, "## " + section);
        }
        return BLANK_LINES.splitAsStream(text).map(paragraph -> paragraph.strip().replaceAll("\\s*\\n\\s*", " ")).toList();
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
