package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;

import java.nio.file.Path;
import java.util.Optional;

public interface BuildFileReader {

    /**
     * @return empty when the project has no build file this reader understands
     * @throws dev.mauricioandrade.shisho.domain.exception.BuildFileException if the build file
     *         exists but cannot be read
     */
    Optional<BuildInfo> read(Path projectRoot);
}
