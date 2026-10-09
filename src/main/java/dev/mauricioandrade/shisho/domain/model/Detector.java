package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * Measures one signal named in the catalog's {@code sinais_overengineering} or
 * {@code sinais_subengenharia}. The id is the one the catalog uses.
 */
public interface Detector {

    String id();

    /** @return the evidence when the signal is present in the project */
    Optional<Evidence> detect(ProjectModel project);

    default Optional<Evidence> evidence(Object... arguments) {
        return Optional.of(new Evidence(id(), List.of(arguments)));
    }
}
