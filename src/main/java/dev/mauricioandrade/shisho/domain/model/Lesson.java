package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;

/** One violation found by a kata, explained through an i18n message key. */
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
