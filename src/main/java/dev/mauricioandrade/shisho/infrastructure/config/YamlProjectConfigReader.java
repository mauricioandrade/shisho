package dev.mauricioandrade.shisho.infrastructure.config;

import dev.mauricioandrade.shisho.domain.exception.ConfigException;
import dev.mauricioandrade.shisho.domain.model.ProjectConfig;
import dev.mauricioandrade.shisho.domain.model.ProjectConfig.KataSetting;
import dev.mauricioandrade.shisho.domain.model.ProjectContext;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.port.out.ProjectConfigReader;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads {@code shisho.yaml} from the project root. Field names are in Portuguese, like the
 * catalog's. Unknown fields are errors: a typo would otherwise silently configure nothing.
 * <pre>
 * katas:
 *   system-out:
 *     ativa: false
 *   dto-not-record:
 *     severidade: info
 * contexto:
 *   tipo: mvp              # mvp | portfolio | produto | corporativo
 *   time: 3                # people on the project
 *   vida_util: longa       # curta | media | longa
 *   caracteristicas: [dominio_com_regras_complexas]
 * </pre>
 * The context becomes catalog keys: {@code tipo: mvp} is {@code mvp}, {@code vida_util: longa} is
 * {@code vida_util_longa}, a team of {@value #LARGE_TEAM} or more is {@code time_grande}, and
 * {@code caracteristicas} lists any other key directly.
 */
public final class YamlProjectConfigReader implements ProjectConfigReader {

    static final String FILE = "shisho.yaml";
    private static final String KATAS = "katas";
    private static final String CONTEXT = "contexto";
    private static final String TYPE = "tipo";
    private static final String TEAM = "time";
    private static final String LIFESPAN = "vida_util";
    private static final String TRAITS = "caracteristicas";
    private static final Set<String> TYPES = Set.of("mvp", "portfolio", "produto", "corporativo");
    private static final Set<String> LIFESPANS = Set.of("curta", "media", "longa");
    static final int LARGE_TEAM = 10;
    private static final String ENABLED = "ativa";
    private static final String SEVERITY = "severidade";
    private static final Map<String, Severity> SEVERITIES = Map.of(
            "grave", Severity.GRAVE, "media", Severity.MEDIA, "leve", Severity.LEVE, "info", Severity.INFO);

    @Override
    public ProjectConfig read(Path projectRoot) {
        Path file = projectRoot.resolve(FILE);
        if (!Files.isRegularFile(file)) {
            return ProjectConfig.DEFAULT;
        }
        Object document = parse(file);
        if (document == null) {
            return ProjectConfig.DEFAULT;
        }
        if (!(document instanceof Map<?, ?> root)) {
            throw new ConfigException("config.error.invalid-document", FILE);
        }
        checkFields(root, Set.of(KATAS, CONTEXT), "");
        Map<String, KataSetting> katas = new HashMap<>();
        if (root.containsKey(KATAS)) {
            map(root.get(KATAS), KATAS).forEach((id, setting) ->
                    katas.put(String.valueOf(id), kataSetting(setting, KATAS + "." + id)));
        }
        Optional<ProjectContext> context = root.containsKey(CONTEXT)
                ? Optional.of(context(root.get(CONTEXT)))
                : Optional.empty();
        return new ProjectConfig(katas, context);
    }

    private static ProjectContext context(Object value) {
        Map<?, ?> fields = map(value, CONTEXT);
        checkFields(fields, Set.of(TYPE, TEAM, LIFESPAN, TRAITS), CONTEXT + ".");
        Set<String> keys = new HashSet<>();
        oneOf(fields, TYPE, TYPES).filter("mvp"::equals).ifPresent(_ -> keys.add("mvp"));
        oneOf(fields, LIFESPAN, LIFESPANS).filter("longa"::equals).ifPresent(_ -> keys.add("vida_util_longa"));
        switch (fields.get(TEAM)) {
            case null -> {
            }
            case Integer people when people >= 1 -> {
                if (people >= LARGE_TEAM) {
                    keys.add("time_grande");
                }
            }
            case Object other -> throw invalid(CONTEXT + "." + TEAM, other);
        }
        if (fields.containsKey(TRAITS)) {
            if (!(fields.get(TRAITS) instanceof List<?> traits)) {
                throw invalid(CONTEXT + "." + TRAITS, fields.get(TRAITS));
            }
            for (Object trait : traits) {
                if (!ProjectContext.KNOWN_KEYS.contains(String.valueOf(trait))) {
                    throw invalid(CONTEXT + "." + TRAITS, trait);
                }
                keys.add(String.valueOf(trait));
            }
        }
        return new ProjectContext(keys);
    }

    private static Optional<String> oneOf(Map<?, ?> fields, String name, Set<String> allowed) {
        Object value = fields.get(name);
        if (value == null) {
            return Optional.empty();
        }
        String text = String.valueOf(value).toLowerCase(Locale.ROOT);
        if (!allowed.contains(text)) {
            throw invalid(CONTEXT + "." + name, value);
        }
        return Optional.of(text);
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

    private static KataSetting kataSetting(Object value, String path) {
        Map<?, ?> fields = map(value, path);
        checkFields(fields, Set.of(ENABLED, SEVERITY), path + ".");
        boolean enabled = switch (fields.get(ENABLED)) {
            case null -> true;
            case Boolean flag -> flag;
            case Object other -> throw invalid(path + "." + ENABLED, other);
        };
        Optional<Severity> severity = Optional.ofNullable(fields.get(SEVERITY)).map(name ->
                Optional.ofNullable(SEVERITIES.get(String.valueOf(name).toLowerCase(Locale.ROOT)))
                        .orElseThrow(() -> invalid(path + "." + SEVERITY, name)));
        return new KataSetting(enabled, severity);
    }

    private static Map<?, ?> map(Object value, String path) {
        if (value instanceof Map<?, ?> fields) {
            return fields;
        }
        throw invalid(path, value);
    }

    private static void checkFields(Map<?, ?> fields, Set<String> known, String prefix) {
        fields.keySet().stream()
                .map(String::valueOf)
                .filter(name -> !known.contains(name))
                .sorted()
                .findFirst()
                .ifPresent(unknown -> {
                    throw new ConfigException("config.error.unknown-field", FILE, prefix + unknown);
                });
    }

    private static ConfigException invalid(String path, Object value) {
        return new ConfigException("config.error.invalid-value", FILE, path, String.valueOf(value));
    }
}
