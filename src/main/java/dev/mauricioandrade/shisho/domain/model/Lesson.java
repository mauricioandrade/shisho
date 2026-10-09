package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;

/**
 * One violation found by a kata, explained through an i18n message key. {@code line} is 0 when
 * the lesson is about the whole file, such as a dependency missing from the build.
 */
public record Lesson(
        String kataId,
        Severity severity,
        Path file,
        int line,
        String messageKey,
        List<Object> arguments) {

    public Lesson {
        arguments = List.copyOf(arguments);
    }
}
