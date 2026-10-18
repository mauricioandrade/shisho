package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.Baseline;

import java.nio.file.Path;

public interface BaselineWriter {

    /**
     * Replaces the project's baseline.
     *
     * @return the file written, relative to {@code projectRoot}
     * @throws dev.mauricioandrade.shisho.domain.exception.ConfigException if it cannot be written
     */
    Path write(Path projectRoot, Baseline baseline);
}
