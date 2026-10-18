package dev.mauricioandrade.shisho.infrastructure.settings;

import dev.mauricioandrade.shisho.domain.model.SettingsFile;
import dev.mauricioandrade.shisho.domain.model.SettingsFile.Setting;
import dev.mauricioandrade.shisho.domain.port.out.SettingsReader;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.lang.System.Logger.Level;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The {@code application.properties} and {@code application.yml} (and {@code application-<profile>.*})
 * in every {@code src/main/resources} and its {@code config/}, the files Spring Boot and Quarkus
 * read. Keys come out flat, as a {@code .properties} file writes them. A setting's profiles come
 * from the file name ({@code application-prod.yml}), from a Quarkus prefix ({@code %prod.}) or from
 * the {@code spring.config.activate.on-profile} of its document ({@code ---} in YAML, {@code #---}
 * in a {@code .properties}). See docs/adr/0024-settings-katas.md.
 */
public final class ApplicationSettingsReader implements SettingsReader {

    private static final System.Logger LOG = System.getLogger(ApplicationSettingsReader.class.getName());
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Set<String> IGNORED_DIRECTORIES = Set.of("target", "build", "out", "node_modules");
    private static final Pattern FILE_NAME = Pattern.compile("application(?:-(?<profile>[\\w.-]+))?\\.(?<format>properties|ya?ml)");
    private static final Pattern PROFILE_PREFIX = Pattern.compile("%(?<profiles>[\\w,-]+)\\.(?<key>.+)");
    private static final Pattern PROFILE_SEPARATORS = Pattern.compile("[\\s,|&()]+");
    private static final Pattern BRACKETED_KEY = Pattern.compile("\\.?\\[(?<name>[^\\]]*\\D[^\\]]*)]");
    private static final List<String> DOCUMENT_PROFILE_KEYS = List.of("spring.config.activate.on-profile", "spring.profiles");
    private static final List<String> DOCUMENT_SEPARATORS = List.of("#---", "!---");

    @Override
    public List<SettingsFile> read(Path projectRoot) {
        List<SettingsFile> files = new ArrayList<>();
        for (Path file : settingsFiles(projectRoot)) {
            try {
                files.add(new SettingsFile(projectRoot.relativize(file), settings(file)));
            } catch (IOException | YAMLException e) {
                LOG.log(Level.DEBUG, "Skipping the settings file {0}: {1}", file, e.getMessage());
            }
        }
        return files;
    }

    private static List<Path> settingsFiles(Path projectRoot) {
        List<Path> files = new ArrayList<>();
        try {
            Files.walkFileTree(projectRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
                    if (!directory.equals(projectRoot) && skipped(projectRoot.relativize(directory))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (directory.endsWith(RESOURCES)) {
                        collect(directory, files);
                        collect(directory.resolve("config"), files);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        files.sort(Comparator.naturalOrder());
        return files;
    }

    /** Hidden directories, and build output outside {@code src}. */
    private static boolean skipped(Path relativeDirectory) {
        String name = relativeDirectory.getFileName().toString();
        if (name.startsWith(".")) {
            return true;
        }
        boolean insideSources = false;
        for (Path part : relativeDirectory) {
            insideSources |= part.toString().equals("src");
        }
        return !insideSources && IGNORED_DIRECTORIES.contains(name);
    }

    private static void collect(Path directory, List<Path> files) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> children = Files.list(directory)) {
            children.filter(Files::isRegularFile)
                    .filter(file -> FILE_NAME.matcher(file.getFileName().toString()).matches())
                    .forEach(files::add);
        }
    }

    private static List<Setting> settings(Path file) throws IOException {
        Matcher name = FILE_NAME.matcher(file.getFileName().toString());
        if (!name.matches()) {
            throw new IllegalStateException("not a settings file: " + file);
        }
        List<String> fileProfiles = name.group("profile") == null ? List.of() : List.of(name.group("profile"));
        String text = Files.readString(file);
        List<List<Raw>> documents = name.group("format").equals("properties") ? properties(text) : yaml(text);
        List<Setting> settings = new ArrayList<>();
        for (List<Raw> document : documents) {
            List<String> documentProfiles = document.stream()
                    .filter(raw -> DOCUMENT_PROFILE_KEYS.contains(raw.key()))
                    .flatMap(raw -> PROFILE_SEPARATORS.splitAsStream(raw.value().trim()))
                    .filter(profile -> !profile.isEmpty())
                    .toList();
            for (Raw raw : document) {
                settings.add(setting(raw, Stream.concat(fileProfiles.stream(), documentProfiles.stream()).toList()));
            }
        }
        return settings;
    }

    /** A Quarkus {@code %dev,test.} prefix adds its profiles and leaves the key without it. */
    private static Setting setting(Raw raw, List<String> profiles) {
        String key = BRACKETED_KEY.matcher(raw.key()).replaceAll(match -> (match.group().startsWith(".") ? "." : "")
                + Matcher.quoteReplacement(match.group("name")));
        Matcher prefix = PROFILE_PREFIX.matcher(key);
        if (!prefix.matches()) {
            return new Setting(key, raw.value(), raw.line(), profiles);
        }
        List<String> all = Stream.concat(profiles.stream(), Arrays.stream(prefix.group("profiles").split(","))).toList();
        return new Setting(prefix.group("key"), raw.value(), raw.line(), all);
    }

    /** A key and value as the file writes them, before profiles. */
    private record Raw(String key, String value, int line) {
    }

    /** The documents of a {@code .properties} file, read the way {@link java.util.Properties} reads it. */
    private static List<List<Raw>> properties(String text) {
        List<List<Raw>> documents = new ArrayList<>();
        List<Raw> document = new ArrayList<>();
        String[] lines = text.split("\r\n|\r|\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int line = i + 1;
            String logical = lines[i].stripLeading();
            if (DOCUMENT_SEPARATORS.contains(logical.strip())) {
                documents.add(document);
                document = new ArrayList<>();
                continue;
            }
            if (logical.isEmpty() || logical.startsWith("#") || logical.startsWith("!")) {
                continue;
            }
            while (continues(logical) && i + 1 < lines.length) {
                logical = logical.substring(0, logical.length() - 1) + lines[++i].stripLeading();
            }
            document.add(pair(logical, line));
        }
        documents.add(document);
        return documents;
    }

    private static boolean continues(String line) {
        int backslashes = 0;
        for (int i = line.length() - 1; i >= 0 && line.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    private static Raw pair(String line, int number) {
        int end = 0;
        while (end < line.length()) {
            char c = line.charAt(end);
            if (c == '\\') {
                end += 2;
                continue;
            }
            if (c == '=' || c == ':' || Character.isWhitespace(c)) {
                break;
            }
            end++;
        }
        end = Math.min(end, line.length());
        int value = end;
        while (value < line.length() && Character.isWhitespace(line.charAt(value))) {
            value++;
        }
        if (value < line.length() && (line.charAt(value) == '=' || line.charAt(value) == ':')) {
            value++;
        }
        while (value < line.length() && Character.isWhitespace(line.charAt(value))) {
            value++;
        }
        return new Raw(unescape(line.substring(0, end)), unescape(line.substring(value)), number);
    }

    private static String unescape(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\\' || i + 1 == text.length()) {
                result.append(c);
                continue;
            }
            char next = text.charAt(++i);
            switch (next) {
                case 't' -> result.append('\t');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 'f' -> result.append('\f');
                case 'u' -> {
                    if (i + 4 < text.length() && text.substring(i + 1, i + 5).matches("\\p{XDigit}{4}")) {
                        result.append((char) Integer.parseInt(text.substring(i + 1, i + 5), 16));
                        i += 4;
                    } else {
                        result.append(next);
                    }
                }
                default -> result.append(next);
            }
        }
        return result.toString();
    }

    /** The documents of a YAML file, with nested keys joined by dots and list items as {@code key[0]}. */
    private static List<List<Raw>> yaml(String text) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(true);
        List<List<Raw>> documents = new ArrayList<>();
        for (Node root : new Yaml(new SafeConstructor(options)).composeAll(new StringReader(text))) {
            List<Raw> document = new ArrayList<>();
            flatten(root, "", root.getStartMark().getLine() + 1, document);
            documents.add(document);
        }
        return documents;
    }

    private static void flatten(Node node, String key, int line, List<Raw> settings) {
        switch (node) {
            case MappingNode mapping -> {
                for (NodeTuple tuple : mapping.getValue()) {
                    if (tuple.getKeyNode() instanceof ScalarNode name && !name.getValue().equals("<<")) {
                        String child = key.isEmpty() ? name.getValue() : key + "." + name.getValue();
                        flatten(tuple.getValueNode(), child, name.getStartMark().getLine() + 1, settings);
                    }
                }
            }
            case SequenceNode sequence -> {
                List<Node> items = sequence.getValue();
                for (int i = 0; i < items.size(); i++) {
                    flatten(items.get(i), key + "[" + i + "]", items.get(i).getStartMark().getLine() + 1, settings);
                }
            }
            case ScalarNode scalar -> {
                if (!key.isEmpty()) {
                    settings.add(new Raw(key, scalar.getValue(), line));
                }
            }
            default -> {
            }
        }
    }
}
