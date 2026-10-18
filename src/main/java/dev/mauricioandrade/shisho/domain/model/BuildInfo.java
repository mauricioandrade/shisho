package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * What the analyzed project's build file declares. {@code file} is relative to the project root.
 * {@code javaVersion} is empty when the build does not say it locally (e.g. it is inherited from a
 * remote parent). {@code dependencies} are the ones declared in {@code file}; a multi-module build
 * lists every module, nested ones included, in {@code modules}, each with its own file.
 * {@code platformVersions} are the versions {@code file} pins for whole platforms, by groupId: a
 * parent, an imported BOM, a build plugin. Only literal versions, or properties of the same file.
 */
public record BuildInfo(Path file, Optional<JavaVersion> javaVersion, List<Dependency> dependencies, List<BuildInfo> modules,
                        Map<String, String> platformVersions) {

    public BuildInfo {
        dependencies = List.copyOf(dependencies);
        modules = List.copyOf(modules);
        platformVersions = Map.copyOf(platformVersions);
    }

    public BuildInfo(Path file, Optional<JavaVersion> javaVersion, List<Dependency> dependencies, List<BuildInfo> modules) {
        this(file, javaVersion, dependencies, modules, Map.of());
    }

    public BuildInfo(Path file, Optional<JavaVersion> javaVersion, List<Dependency> dependencies) {
        this(file, javaVersion, dependencies, List.of());
    }

    /** The version this build, or the first of its modules that says it, pins for {@code groupId}. */
    public Optional<String> platformVersion(String groupId) {
        return withModules().flatMap(build -> Optional.ofNullable(build.platformVersions().get(groupId)).stream()).findFirst();
    }

    /** This build and its modules. */
    public Stream<BuildInfo> withModules() {
        return Stream.concat(Stream.of(this), modules.stream().flatMap(BuildInfo::withModules));
    }

    /** The dependencies of the whole project: this build's and every module's, without repeats. */
    public List<Dependency> allDependencies() {
        return withModules().flatMap(build -> build.dependencies().stream()).distinct().toList();
    }

    /** Whether this build or any of its modules declares the dependency. */
    public boolean dependsOn(String groupId, String artifactId) {
        return allDependencies().contains(new Dependency(groupId, artifactId));
    }

    /** The build file that declares {@code dependency}, where a lesson about it belongs; {@code file} if none does. */
    public Path fileDeclaring(Dependency dependency) {
        return withModules().filter(build -> build.dependencies().contains(dependency)).map(BuildInfo::file).findFirst().orElse(file);
    }

    /**
     * The lowest version any build declares: code in every module has to compile at it, so no kata
     * suggests what one of them cannot use.
     */
    public Optional<JavaVersion> lowestJavaVersion() {
        return withModules().flatMap(build -> build.javaVersion().stream()).min(Comparator.naturalOrder());
    }
}
