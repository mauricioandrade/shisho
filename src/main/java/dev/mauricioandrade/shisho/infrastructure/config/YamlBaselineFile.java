package dev.mauricioandrade.shisho.infrastructure.config;

import dev.mauricioandrade.shisho.domain.exception.ConfigException;
import dev.mauricioandrade.shisho.domain.model.Baseline;
import dev.mauricioandrade.shisho.domain.model.Baseline.Entry;
import dev.mauricioandrade.shisho.domain.port.out.BaselineReader;
import dev.mauricioandrade.shisho.domain.port.out.BaselineWriter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@code shisho-baseline.yaml} at the project root, meant to be committed. Field names are in
 * Portuguese, like the {@code shisho.yaml}'s; there are no lines, so moving code does not change
 * the file:
 * <pre>
 * licoes:
 * - kata: field-injection
 *   arquivo: src/main/java/app/UserController.java
 *   mensagem: kata.field-injection.lesson
 *   argumentos: [repository]
 * </pre>
 */
public final class YamlBaselineFile implements BaselineReader, BaselineWriter {

    static final String FILE = "shisho-baseline.yaml";
    private static final String LESSONS = "licoes";
    private static final String KATA = "kata";
    private static final String PATH = "arquivo";
    private static final String MESSAGE = "mensagem";
    private static final String ARGUMENTS = "argumentos";
    private static final Set<String> FIELDS = Set.of(KATA, PATH, MESSAGE, ARGUMENTS);

    @Override
    public Optional<Baseline> read(Path projectRoot) {
        Path file = projectRoot.resolve(FILE);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        if (!(parse(file) instanceof Map<?, ?> root) || !root.keySet().equals(Set.of(LESSONS))
                || !(root.get(LESSONS) instanceof List<?> lessons)) {
            throw invalid(LESSONS);
        }
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < lessons.size(); i++) {
            entries.add(entry(lessons.get(i), LESSONS + "[" + i + "]"));
        }
        return Optional.of(new Baseline(entries));
    }

    @Override
    public Path write(Path projectRoot, Baseline baseline) {
        List<Map<String, Object>> lessons = baseline.entries().stream().map(entry -> {
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put(KATA, entry.kata());
            fields.put(PATH, entry.file());
            fields.put(MESSAGE, entry.message());
            fields.put(ARGUMENTS, entry.arguments());
            return fields;
        }).toList();
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.AUTO);
        options.setWidth(Integer.MAX_VALUE);
        options.setAllowUnicode(true);
        try {
            Files.writeString(projectRoot.resolve(FILE), new Yaml(options).dump(Map.of(LESSONS, lessons)));
        } catch (IOException e) {
            throw new ConfigException("baseline.error.write", e, FILE);
        }
        return Path.of(FILE);
    }

    private static Object parse(Path file) {
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        try {
            return new Yaml(new SafeConstructor(options)).load(Files.readString(file));
        } catch (YAMLException e) {
            throw new ConfigException("config.error.invalid-yaml", e, FILE, e.getMessage());
        } catch (IOException e) {
            throw new ConfigException("config.error.read", e, FILE);
        }
    }

    private static Entry entry(Object value, String path) {
        if (!(value instanceof Map<?, ?> fields) || !fields.keySet().equals(FIELDS)) {
            throw invalid(path);
        }
        if (!(fields.get(ARGUMENTS) instanceof List<?> arguments)) {
            throw invalid(path + "." + ARGUMENTS);
        }
        return new Entry(text(fields.get(KATA), path + "." + KATA), text(fields.get(PATH), path + "." + PATH),
                text(fields.get(MESSAGE), path + "." + MESSAGE), arguments.stream().map(String::valueOf).toList());
    }

    private static String text(Object value, String path) {
        if (!(value instanceof String text)) {
            throw invalid(path);
        }
        return text;
    }

    private static ConfigException invalid(String path) {
        return new ConfigException("baseline.error.invalid", FILE, path);
    }
}
