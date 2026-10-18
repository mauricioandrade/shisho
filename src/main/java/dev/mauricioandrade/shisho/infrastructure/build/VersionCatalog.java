package dev.mauricioandrade.shisho.infrastructure.build;

import dev.mauricioandrade.shisho.domain.exception.BuildFileException;
import dev.mauricioandrade.shisho.domain.model.Dependency;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The {@code [libraries]} and {@code [bundles]} of Gradle's version catalogs, by the accessor a build
 * script uses: in the default catalog, {@code gradle/libs.versions.toml}, the alias
 * {@code spring-boot-starter-web} is {@code libs.spring.boot.starter.web} and the bundle {@code web}
 * is {@code libs.bundles.web}; a catalog the settings name {@code spring} gives {@code spring.*}.
 * Read line by line: TOML 1.0 keeps an inline table on one line, which is the only shape a library
 * takes; a bundle's array is joined until its {@code ]}.
 */
final class VersionCatalog {

    static final Path FILE = Path.of("gradle", "libs.versions.toml");

    private static final Pattern TABLE = Pattern.compile("^\\s*\\[\\s*([\\w.-]+)\\s*]");

    /** {@code alias = ...}, with the alias optionally quoted. */
    private static final Pattern ENTRY = Pattern.compile("^\\s*[\"']?([\\w.-]+)[\"']?\\s*=\\s*(.+)$");

    /** {@code "g:a[:v]"} or {@code { module = "g:a", ... }}. */
    private static final Pattern MODULE = Pattern.compile("^(?:\\{.*\\bmodule\\s*=\\s*)?[\"']([\\w.-]+):([\\w.-]+)(?::[^\"']*)?[\"']");

    private static final Pattern GROUP = Pattern.compile("\\bgroup\\s*=\\s*[\"']([\\w.-]+)[\"']");
    private static final Pattern NAME = Pattern.compile("\\bname\\s*=\\s*[\"']([\\w.-]+)[\"']");

    /** A library alias inside a bundle's array. */
    private static final Pattern MEMBER = Pattern.compile("[\"']([\\w.-]+)[\"']");

    private VersionCatalog() {
    }

    /**
     * The dependencies by accessor, for the catalogs in {@code files} (name to file, relative to the
     * project root): one for a library, those of its members for a bundle ({@code libs.bundles.web}).
     * A catalog whose file is missing has none.
     */
    static Map<String, List<Dependency>> dependencies(Path projectRoot, Map<String, Path> files) {
        Map<String, List<Dependency>> dependencies = new HashMap<>();
        files.forEach((name, file) -> read(projectRoot.resolve(file))
                .forEach((accessor, members) -> dependencies.put(name + "." + accessor, members)));
        return Map.copyOf(dependencies);
    }

    /** The dependencies of one catalog by accessor without its name. */
    private static Map<String, List<Dependency>> read(Path file) {
        if (!Files.isRegularFile(file)) {
            return Map.of();
        }
        Map<String, Dependency> libraries = new HashMap<>();
        Map<String, String> bundles = new HashMap<>();
        String table = "";
        String bundle = null;
        for (String line : readFile(file).lines().map(VersionCatalog::withoutComment).toList()) {
            if (bundle != null) {
                bundles.merge(bundle, line, String::concat);
                bundle = line.contains("]") ? null : bundle;
                continue;
            }
            Matcher header = TABLE.matcher(line);
            if (header.find()) {
                table = header.group(1);
                continue;
            }
            Matcher entry = ENTRY.matcher(line);
            if (!entry.find()) {
                continue;
            }
            String value = entry.group(2).strip();
            if (table.equals("libraries")) {
                library(value).ifPresent(library -> libraries.put(accessor(entry.group(1)), library));
            } else if (table.equals("bundles")) {
                bundles.put(accessor(entry.group(1)), value);
                bundle = value.contains("]") ? null : accessor(entry.group(1));
            }
        }
        Map<String, List<Dependency>> dependencies = new HashMap<>();
        libraries.forEach((accessor, library) -> dependencies.put(accessor, List.of(library)));
        bundles.forEach((accessor, members) -> dependencies.put("bundles." + accessor, members(members, libraries)));
        return Map.copyOf(dependencies);
    }

    /** The libraries a bundle names, in its order; a name missing from the libraries is skipped. */
    private static List<Dependency> members(String array, Map<String, Dependency> libraries) {
        List<Dependency> members = new ArrayList<>();
        Matcher member = MEMBER.matcher(array);
        while (member.find()) {
            Dependency library = libraries.get(accessor(member.group(1)));
            if (library != null) {
                members.add(library);
            }
        }
        return List.copyOf(members);
    }

    /** Gradle turns {@code -}, {@code _} and {@code .} in an alias into the dots of the accessor. */
    static String accessor(String alias) {
        return alias.replace('-', '.').replace('_', '.');
    }

    private static Optional<Dependency> library(String value) {
        Matcher module = MODULE.matcher(value);
        if (module.find()) {
            return Optional.of(new Dependency(module.group(1), module.group(2)));
        }
        Matcher group = GROUP.matcher(value);
        Matcher name = NAME.matcher(value);
        return group.find() && name.find() ? Optional.of(new Dependency(group.group(1), name.group(1))) : Optional.empty();
    }

    /** Everything after a {@code #} outside a string. */
    private static String withoutComment(String line) {
        Character quote = null;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote == null && c == '#') {
                return line.substring(0, i);
            }
            if (c == '"' || c == '\'') {
                quote = quote == null ? Character.valueOf(c) : quote == c ? null : quote;
            }
        }
        return line;
    }

    private static String readFile(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new BuildFileException("build.error.read", e, file.toString());
        }
    }
}
