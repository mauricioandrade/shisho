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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads the Java version and dependencies from a single-module {@code build.gradle.kts} or
 * {@code build.gradle}. The script is never run: it is code from an untrusted project, and running
 * it would need Gradle and the network. The reader recognizes the literal forms that Spring
 * Initializr, code.quarkus.io and the Gradle docs write; a version or a dependency computed by the
 * script (a variable, a version catalog) is not seen. See {@code docs/adr/0013-gradle.md}.
 */
public final class GradleBuildFileReader implements BuildFileReader {

    /** When a project has both, the Kotlin DSL is read. */
    private static final List<String> SCRIPTS = List.of("build.gradle.kts", "build.gradle");

    /** {@code options.release = 21} or {@code options.release.set(21)} on {@code JavaCompile}. */
    private static final Pattern RELEASE = Pattern.compile("\\boptions\\.release(?:\\s*=\\s*|\\.set\\(\\s*)(\\d+)");

    /** {@code languageVersion = JavaLanguageVersion.of(21)} in {@code java { toolchain { } }}. */
    private static final Pattern TOOLCHAIN = Pattern.compile("\\bJavaLanguageVersion\\.of\\(\\s*['\"]?(\\d+)['\"]?\\s*\\)");

    /** {@code JavaVersion.VERSION_21}, {@code JavaVersion.toVersion(21)}, {@code '21'} or {@code 21}. */
    private static final Pattern SOURCE_COMPATIBILITY = Pattern.compile("\\bsourceCompatibility\\s*=\\s*"
            + "(?:JavaVersion\\.VERSION_(\\d+(?:_\\d+)?)|JavaVersion\\.toVersion\\(\\s*['\"]?([\\d.]+)['\"]?\\s*\\)|['\"]?([\\d.]+)['\"]?)");

    /**
     * A configuration followed by {@code 'group:artifact[:version]'} or by
     * {@code group: 'g', name: 'a'} (Groovy) / {@code group = "g", name = "a"} (Kotlin).
     */
    private static final Pattern DEPENDENCY = Pattern.compile("\\b(?<configuration>[A-Za-z]\\w*)\\s*\\(?\\s*(?:"
            + "(?<q>['\"])(?<group>[\\w.-]+):(?<artifact>[\\w.-]+)(?::[^'\"]*)?\\k<q>"
            + "|group\\s*[:=]\\s*(?<q2>['\"])(?<mapGroup>[\\w.-]+)\\k<q2>\\s*,\\s*name\\s*[:=]\\s*(?<q3>['\"])(?<mapArtifact>[\\w.-]+)\\k<q3>)");

    /** BOMs, like {@code dependencyManagement} in Maven: they manage versions, they add no library. */
    private static final Set<String> PLATFORMS = Set.of("platform", "enforcedPlatform");

    private static final Pattern DEPENDENCIES_BLOCK = Pattern.compile("\\bdependencies\\s*\\{");

    @Override
    public Optional<BuildInfo> read(Path projectRoot) {
        return SCRIPTS.stream()
                .filter(script -> Files.isRegularFile(projectRoot.resolve(script)))
                .findFirst()
                .map(script -> {
                    String code = withoutComments(readScript(projectRoot.resolve(script)));
                    return new BuildInfo(Path.of(script), javaVersion(projectRoot.resolve(script), code), dependencies(code));
                });
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

    /** Only the top-level {@code dependencies} block: {@code buildscript}'s holds Gradle plugins. */
    private static List<Dependency> dependencies(String code) {
        List<Dependency> dependencies = new ArrayList<>();
        Matcher block = DEPENDENCIES_BLOCK.matcher(code);
        while (block.find()) {
            if (depth(code, block.start()) != 0) {
                continue;
            }
            Matcher dependency = DEPENDENCY.matcher(code.substring(block.end(), closingBrace(code, block.end())));
            while (dependency.find()) {
                if (PLATFORMS.contains(dependency.group("configuration"))) {
                    continue;
                }
                dependencies.add(dependency.group("group") != null
                        ? new Dependency(dependency.group("group"), dependency.group("artifact"))
                        : new Dependency(dependency.group("mapGroup"), dependency.group("mapArtifact")));
            }
        }
        return dependencies.stream().distinct().toList();
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
