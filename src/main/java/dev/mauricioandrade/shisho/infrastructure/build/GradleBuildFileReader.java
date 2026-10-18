package dev.mauricioandrade.shisho.infrastructure.build;

import dev.mauricioandrade.shisho.domain.exception.BuildFileException;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.port.out.BuildFileReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads the Java version and dependencies from a {@code build.gradle.kts} or {@code build.gradle},
 * from the scripts of the projects that {@code settings.gradle(.kts)} includes and from the
 * convention plugins of {@code buildSrc} and of included builds. The script is never run: it is code
 * from an untrusted project, and running it would need Gradle and the network. The reader recognizes
 * the literal forms that Spring Initializr, code.quarkus.io and the Gradle docs write, plus the
 * libraries and bundles of the version catalogs; a version or a dependency computed by the
 * script (a variable, an {@code if}) is not seen.
 * See {@code docs/adr/0013-gradle.md} and {@code docs/adr/0015-multi-module.md}.
 */
public final class GradleBuildFileReader implements BuildFileReader {

    /** When a project has both, the Kotlin DSL is read. */
    private static final List<String> SCRIPTS = List.of("build.gradle.kts", "build.gradle");

    private static final List<String> SETTINGS = List.of("settings.gradle.kts", "settings.gradle");

    /** {@code include("a", "b:c")}, over several lines in Kotlin, or {@code include 'a', 'b'} in Groovy. */
    private static final Pattern INCLUDE = Pattern.compile("\\binclude\\b\\s*(\\([^)]*\\)|[^\\n]*)");

    /** A project path in an {@code include}: {@code ":lib:core"} is the directory {@code lib/core}. */
    private static final Pattern PROJECT_PATH = Pattern.compile("['\"]:?([\\w.-]+(?::[\\w.-]+)*)['\"]");

    /**
     * {@code project(":app").projectDir = file("modules/app")}, or {@code File(rootDir, "modules/app")}
     * ({@code new File(settingsDir, ...)} in Groovy): the project lives somewhere else than its path says.
     */
    private static final Pattern PROJECT_DIR = Pattern.compile("\\bproject\\(\\s*['\"]:?([\\w.-]+(?::[\\w.-]+)*)['\"]\\s*\\)"
            + "\\.projectDir\\s*=\\s*(?:file\\(|(?:new\\s+)?File\\(\\s*(?:rootDir|settingsDir)\\s*,)\\s*['\"]([^'\"]+)['\"]\\s*\\)");

    /** {@code includeBuild("build-logic")}, a build of its own: its convention plugins are read, not its code. */
    private static final Pattern INCLUDE_BUILD = Pattern.compile("\\bincludeBuild\\s*\\(?\\s*['\"]([^'\"]+)['\"]");

    /** Where Gradle compiles a precompiled script plugin from, in {@code buildSrc} or an included build. */
    private static final List<Path> PLUGIN_SOURCES = List.of(Path.of("src/main/kotlin"), Path.of("src/main/groovy"));

    private static final List<String> SCRIPT_SUFFIXES = List.of(".gradle.kts", ".gradle");

    /**
     * {@code create("spring") { from(files("gradle/spring.versions.toml")) }} in Kotlin, or
     * {@code spring { from(files('...')) }} in Groovy, in {@code versionCatalogs { }}.
     */
    private static final Pattern CATALOG = Pattern.compile("(?:\\bcreate\\s*\\(\\s*['\"](\\w+)['\"]\\s*\\)|\\b(\\w+))"
            + "\\s*\\{\\s*from\\s*\\(?\\s*files\\s*\\(\\s*['\"]([^'\"]+)['\"]");

    /** {@code options.release = 21} or {@code options.release.set(21)} on {@code JavaCompile}. */
    private static final Pattern RELEASE = Pattern.compile("\\boptions\\.release(?:\\s*=\\s*|\\.set\\(\\s*)(\\d+)");

    /** {@code languageVersion = JavaLanguageVersion.of(21)} in {@code java { toolchain { } }}. */
    private static final Pattern TOOLCHAIN = Pattern.compile("\\bJavaLanguageVersion\\.of\\(\\s*['\"]?(\\d+)['\"]?\\s*\\)");

    /** {@code JavaVersion.VERSION_21}, {@code JavaVersion.toVersion(21)}, {@code '21'} or {@code 21}. */
    private static final Pattern SOURCE_COMPATIBILITY = Pattern.compile("\\bsourceCompatibility\\s*=\\s*"
            + "(?:JavaVersion\\.VERSION_(\\d+(?:_\\d+)?)|JavaVersion\\.toVersion\\(\\s*['\"]?([\\d.]+)['\"]?\\s*\\)|['\"]?([\\d.]+)['\"]?)");

    /**
     * A configuration followed by {@code 'group:artifact[:version]'}, by
     * {@code group: 'g', name: 'a'} (Groovy) / {@code group = "g", name = "a"} (Kotlin) or by a
     * version catalog accessor, {@code libs.spring.boot.starter.web}: any {@code name.alias}, looked
     * up among the accessors of the catalogs.
     */
    private static final Pattern DEPENDENCY = Pattern.compile("\\b(?<configuration>[A-Za-z]\\w*)\\s*\\(?\\s*(?:"
            + "(?<q>['\"])(?<group>[\\w.-]+):(?<artifact>[\\w.-]+)(?::[^'\"]*)?\\k<q>"
            + "|group\\s*[:=]\\s*(?<q2>['\"])(?<mapGroup>[\\w.-]+)\\k<q2>\\s*,\\s*name\\s*[:=]\\s*(?<q3>['\"])(?<mapArtifact>[\\w.-]+)\\k<q3>"
            + "|(?<accessor>[A-Za-z]\\w*\\.[\\w.]+))");

    /** BOMs, like {@code dependencyManagement} in Maven: they manage versions, they add no library. */
    private static final Set<String> PLATFORMS = Set.of("platform", "enforcedPlatform");

    private static final Pattern DEPENDENCIES_BLOCK = Pattern.compile("\\bdependencies\\s*\\{");

    /** Blocks of the root script whose {@code dependencies} every module gets. */
    private static final Pattern SHARED_BLOCK = Pattern.compile("\\b(?:subprojects|allprojects)\\s*\\{");

    /**
     * A multi-project root may have no script of its own, only the settings: the build is then the
     * settings file, with the included projects as modules.
     */
    @Override
    public Optional<BuildInfo> read(Path projectRoot) {
        Optional<Path> settings = existing(projectRoot, Path.of(""), SETTINGS);
        Map<String, List<Dependency>> catalog = VersionCatalog.dependencies(projectRoot, catalogs(projectRoot, settings));
        Stream<BuildInfo> projects = settings.stream()
                .flatMap(file -> included(projectRoot, file))
                .flatMap(directory -> script(projectRoot, directory, catalog).stream());
        Stream<BuildInfo> conventions = Stream.concat(Stream.of(Path.of("buildSrc")),
                        settings.stream().flatMap(file -> includedBuilds(projectRoot, file)))
                .distinct()
                .flatMap(build -> conventionPlugins(projectRoot, build))
                .map(plugin -> build(projectRoot, plugin, catalog));
        List<BuildInfo> modules = Stream.concat(projects, conventions).toList();
        return script(projectRoot, Path.of(""), catalog)
                .map(root -> new BuildInfo(root.file(), root.javaVersion(), root.dependencies(), modules))
                .or(() -> settings.map(file -> new BuildInfo(file, Optional.empty(), List.of(), modules)));
    }

    private static Optional<BuildInfo> script(Path projectRoot, Path directory, Map<String, List<Dependency>> catalog) {
        return existing(projectRoot, directory, SCRIPTS).map(script -> build(projectRoot, script, catalog));
    }

    private static BuildInfo build(Path projectRoot, Path script, Map<String, List<Dependency>> catalog) {
        String code = withoutComments(readScript(projectRoot.resolve(script)));
        return new BuildInfo(script, javaVersion(projectRoot.resolve(script), code), dependencies(code, catalog));
    }

    /**
     * The version catalogs by name: {@code libs}, from {@code gradle/libs.versions.toml}, plus those
     * the settings create from a file inside the project. One from a published artifact is not read.
     */
    private static Map<String, Path> catalogs(Path projectRoot, Optional<Path> settings) {
        Map<String, Path> catalogs = new HashMap<>(Map.of("libs", VersionCatalog.FILE));
        settings.ifPresent(file -> {
            Matcher catalog = CATALOG.matcher(withoutComments(readScript(projectRoot.resolve(file))));
            while (catalog.find()) {
                Path toml = Path.of(catalog.group(3)).normalize();
                if (!toml.isAbsolute() && !toml.startsWith("..")) {
                    catalogs.put(catalog.group(1) != null ? catalog.group(1) : catalog.group(2), toml);
                }
            }
        });
        return catalogs;
    }

    /** The directories of the builds the settings include, inside the project. */
    private static Stream<Path> includedBuilds(Path projectRoot, Path settings) {
        Matcher includeBuild = INCLUDE_BUILD.matcher(withoutComments(readScript(projectRoot.resolve(settings))));
        List<Path> directories = new ArrayList<>();
        while (includeBuild.find()) {
            Path directory = Path.of(includeBuild.group(1)).normalize();
            if (!directory.isAbsolute() && !directory.startsWith("..")) {
                directories.add(directory);
            }
        }
        return directories.stream();
    }

    /**
     * The precompiled script plugins of {@code build}, sorted so the result does not depend on the
     * file system. Its own {@code build.gradle(.kts)} is left out: it declares Gradle plugins, not
     * libraries of the project. Every convention counts, applied by a module or not.
     */
    private static Stream<Path> conventionPlugins(Path projectRoot, Path build) {
        List<Path> plugins = new ArrayList<>();
        for (Path sources : PLUGIN_SOURCES) {
            Path directory = projectRoot.resolve(build).resolve(sources);
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(directory)) {
                files.filter(Files::isRegularFile)
                        .filter(file -> SCRIPT_SUFFIXES.stream().anyMatch(file.getFileName().toString()::endsWith))
                        .map(projectRoot::relativize)
                        .forEach(plugins::add);
            } catch (IOException e) {
                throw new BuildFileException("build.error.read", e, directory.toString());
            }
        }
        return plugins.stream().sorted();
    }

    /** The first of {@code names} that exists in {@code directory}, relative to the project root. */
    private static Optional<Path> existing(Path projectRoot, Path directory, List<String> names) {
        return names.stream()
                .map(directory::resolve)
                .filter(file -> Files.isRegularFile(projectRoot.resolve(file)))
                .findFirst();
    }

    /**
     * The directories of the included projects, in the order of the settings, inside the project. A
     * {@code projectDir} set in the settings wins over the directory the project path gives.
     */
    private static Stream<Path> included(Path projectRoot, Path settings) {
        String code = withoutComments(readScript(projectRoot.resolve(settings)));
        Map<String, String> projectDirs = new HashMap<>();
        Matcher projectDir = PROJECT_DIR.matcher(code);
        while (projectDir.find()) {
            projectDirs.put(projectDir.group(1), projectDir.group(2));
        }
        List<Path> directories = new ArrayList<>();
        Matcher include = INCLUDE.matcher(code);
        while (include.find()) {
            Matcher path = PROJECT_PATH.matcher(include.group(1));
            while (path.find()) {
                Path directory = Path.of(projectDirs.getOrDefault(path.group(1), path.group(1).replace(':', '/'))).normalize();
                if (!directory.isAbsolute() && !directory.startsWith("..") && !directories.contains(directory)) {
                    directories.add(directory);
                }
            }
        }
        return directories.stream();
    }

    /**
     * Candidates in the order Gradle gives them precedence: {@code options.release} decides the
     * language level the compiler accepts; without it, the toolchain's version is the default for
     * {@code sourceCompatibility}, which comes last.
     */
    private static Optional<JavaVersion> javaVersion(Path script, String code) {
        return Stream.of(first(RELEASE, code), first(TOOLCHAIN, code), first(SOURCE_COMPATIBILITY, code))
                .flatMap(Optional::stream)
                .findFirst()
                .map(candidate -> JavaVersion.parse(candidate).orElseThrow(() ->
                        new BuildFileException("build.error.invalid-java-version", script.toString(), candidate)));
    }

    /** The first non-empty group of the first match, with {@code VERSION_1_8} read as {@code 1.8}. */
    private static Optional<String> first(Pattern pattern, String code) {
        Matcher matcher = pattern.matcher(code);
        if (!matcher.find()) {
            return Optional.empty();
        }
        for (int group = 1; group <= matcher.groupCount(); group++) {
            if (matcher.group(group) != null) {
                return Optional.of(matcher.group(group).replace('_', '.'));
            }
        }
        return Optional.empty();
    }

    /**
     * Only the top-level {@code dependencies} block and those of {@code subprojects} and
     * {@code allprojects}, which the root declares for its modules: {@code buildscript}'s holds
     * Gradle plugins. An accessor missing from {@code catalog} (a typo, another catalog) is skipped.
     */
    private static List<Dependency> dependencies(String code, Map<String, List<Dependency>> catalog) {
        List<Dependency> dependencies = new ArrayList<>();
        for (String block : topLevelBlocks(code, DEPENDENCIES_BLOCK)) {
            Matcher dependency = DEPENDENCY.matcher(block);
            while (dependency.find()) {
                if (PLATFORMS.contains(dependency.group("configuration"))) {
                    continue;
                }
                if (dependency.group("group") != null) {
                    dependencies.add(new Dependency(dependency.group("group"), dependency.group("artifact")));
                } else if (dependency.group("mapGroup") != null) {
                    dependencies.add(new Dependency(dependency.group("mapGroup"), dependency.group("mapArtifact")));
                } else if (dependency.group("accessor") != null) {
                    dependencies.addAll(catalog.getOrDefault(dependency.group("accessor"), List.of()));
                }
            }
        }
        for (String shared : topLevelBlocks(code, SHARED_BLOCK)) {
            dependencies.addAll(dependencies(shared, catalog));
        }
        return dependencies.stream().distinct().toList();
    }

    /** The bodies of the blocks {@code opening} starts outside any other block. */
    private static List<String> topLevelBlocks(String code, Pattern opening) {
        List<String> blocks = new ArrayList<>();
        Matcher block = opening.matcher(code);
        while (block.find()) {
            if (depth(code, block.start()) == 0) {
                blocks.add(code.substring(block.end(), closingBrace(code, block.end())));
            }
        }
        return blocks;
    }

    /** Braces open before {@code position}, outside strings. */
    private static int depth(String code, int position) {
        int depth = 0;
        for (int i = 0; i < position; i = skipString(code, i)) {
            char c = code.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return depth;
    }

    /** The index of the brace that closes the block opened just before {@code start}, or the end. */
    private static int closingBrace(String code, int start) {
        int depth = 1;
        for (int i = start; i < code.length(); i = skipString(code, i)) {
            char c = code.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return code.length();
    }

    /** The index after the string starting at {@code i}, or {@code i + 1} if none starts there. */
    private static int skipString(String code, int i) {
        char quote = code.charAt(i);
        if (quote != '\'' && quote != '"') {
            return i + 1;
        }
        String delimiter = code.startsWith(String.valueOf(quote).repeat(3), i) ? String.valueOf(quote).repeat(3) : String.valueOf(quote);
        int j = i + delimiter.length();
        while (j < code.length() && !code.startsWith(delimiter, j)) {
            j += code.charAt(j) == '\\' ? 2 : 1;
        }
        return Math.min(j + delimiter.length(), code.length());
    }

    /** Comments become a space, so a commented-out dependency is not read. Strings stay intact. */
    private static String withoutComments(String code) {
        StringBuilder result = new StringBuilder(code.length());
        int i = 0;
        while (i < code.length()) {
            if (code.startsWith("//", i)) {
                int end = code.indexOf('\n', i);
                i = end < 0 ? code.length() : end;
                result.append(' ');
            } else if (code.startsWith("/*", i)) {
                int end = code.indexOf("*/", i + 2);
                i = end < 0 ? code.length() : end + 2;
                result.append(' ');
            } else {
                int next = skipString(code, i);
                result.append(code, i, next);
                i = next;
            }
        }
        return result.toString();
    }

    private static String readScript(Path script) {
        try {
            return Files.readString(script);
        } catch (IOException e) {
            throw new BuildFileException("build.error.read", e, script.toString());
        }
    }
}
