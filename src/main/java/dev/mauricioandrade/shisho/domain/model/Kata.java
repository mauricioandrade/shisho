package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * A good-practice rule. Most katas look at one class at a time ({@link ClassKata}); the ones
 * that need the whole project, such as "no class handles errors", are {@link ProjectKata}s.
 */
public sealed interface Kata permits ClassKata, ProjectKata {

    String id();

    Severity severity();

    /**
     * The kata only runs on projects at this Java version or above, unless it suggests a
     * {@link #javaFeature()}: then, below it, its lessons become INFO suggestions for after the upgrade.
     */
    JavaVersion minJavaVersion();

    /** The catalog id of the Java feature this kata suggests ({@code catalog/java-features}), if any. */
    default Optional<String> javaFeature() {
        return Optional.empty();
    }

    /** A lesson of this kata, explained by the message key {@code kata.<id>.lesson}. */
    default Lesson lesson(Path file, int line, Object... arguments) {
        return new Lesson(id(), severity(), file, line, "kata." + id() + ".lesson", List.of(arguments));
    }
}
