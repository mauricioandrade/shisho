package dev.mauricioandrade.shisho.domain.port.in;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ProjectContext;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.Optional;

public interface ClassifyArchitecture {

    /**
     * @param context what the author declared about the project, if anything
     * @return empty when no catalog architecture reaches its minimum number of signals
     */
    Optional<ArchitectureMatch> classify(ProjectModel project, Optional<ProjectContext> context);
}
