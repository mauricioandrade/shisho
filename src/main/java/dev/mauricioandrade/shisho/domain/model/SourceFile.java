package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;

/** Outcome of parsing one source file; {@code file} is relative to the analyzed project root. */
public sealed interface SourceFile {

    Path file();

    record Parsed(Path file, List<ClassUnit> classes) implements SourceFile {

        public Parsed {
            classes = List.copyOf(classes);
        }
    }

    /** {@code problem} is the parser's own diagnostic, shown to the user as-is. */
    record Unparseable(Path file, int line, String problem) implements SourceFile {
    }
}
