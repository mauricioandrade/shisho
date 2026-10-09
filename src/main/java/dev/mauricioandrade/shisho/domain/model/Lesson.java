package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * One violation found by a kata, explained through an i18n message key. {@code line} is 0 when
 * the lesson is about the whole file, such as a dependency missing from the build.
 * {@code afterUpgradeTo} is present when the lesson only applies once the project moves to that
 * Java version: a suggestion, not a violation.
 */
public record Lesson(
        String kataId,
        Severity severity,
        Path file,
        int line,
        String messageKey,
        List<Object> arguments,
        Optional<JavaVersion> afterUpgradeTo) {

    public Lesson {
        arguments = List.copyOf(arguments);
    }

    public Lesson(String kataId, Severity severity, Path file, int line, String messageKey, List<Object> arguments) {
        this(kataId, severity, file, line, messageKey, arguments, Optional.empty());
    }

    public Lesson withSeverity(Severity newSeverity) {
        return new Lesson(kataId, newSeverity, file, line, messageKey, arguments, afterUpgradeTo);
    }

    /** The same lesson as an INFO suggestion for when the project reaches {@code version}. */
    public Lesson afterUpgradeTo(JavaVersion version) {
        return new Lesson(kataId, Severity.INFO, file, line, messageKey, arguments, Optional.of(version));
    }
}
