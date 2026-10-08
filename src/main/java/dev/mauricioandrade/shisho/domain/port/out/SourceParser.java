package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.SourceFile;

import java.nio.file.Path;
import java.util.List;

public interface SourceParser {

    /**
     * Parses the production sources of a project (test sources and build output are skipped),
     * accepting only the syntax available in {@code languageLevel}. Ordered by file path.
     *
     * @throws dev.mauricioandrade.shisho.domain.exception.SourceException if the project
     *         directory cannot be read
     */
    List<SourceFile> parse(Path projectRoot, JavaVersion languageLevel);
}
