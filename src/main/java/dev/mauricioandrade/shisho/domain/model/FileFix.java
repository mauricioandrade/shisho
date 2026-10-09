package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;

/** The corrected content of one file and the lessons the correction fixes. {@code file} is relative to the project. */
public record FileFix(Path file, String original, String fixed, List<Lesson> lessons) {

    public FileFix {
        lessons = List.copyOf(lessons);
    }
}
