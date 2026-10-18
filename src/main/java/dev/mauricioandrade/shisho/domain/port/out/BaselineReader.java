package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.Baseline;

import java.nio.file.Path;
import java.util.Optional;

public interface BaselineReader {

    /**
     * @return empty when the project has no baseline
     * @throws dev.mauricioandrade.shisho.domain.exception.ConfigException if the file exists but
     *         cannot be read or is invalid
     */
    Optional<Baseline> read(Path projectRoot);
}
