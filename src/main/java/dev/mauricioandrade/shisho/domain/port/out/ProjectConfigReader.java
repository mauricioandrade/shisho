package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.ProjectConfig;

import java.nio.file.Path;

public interface ProjectConfigReader {

    /**
     * @return {@link ProjectConfig#DEFAULT} when the project has no configuration file
     * @throws dev.mauricioandrade.shisho.domain.exception.ConfigException if the file exists but
     *         cannot be read or is invalid
     */
    ProjectConfig read(Path projectRoot);
}
