package dev.mauricioandrade.shisho.domain.port.in;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.Optional;

public interface ClassifyArchitecture {

    /** @return empty when no catalog architecture reaches its minimum number of signals */
    Optional<ArchitectureMatch> classify(ProjectModel project);
}
