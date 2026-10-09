package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * What {@code shisho crud} was asked to generate: an entity and its fields, plus the choices
 * that are otherwise taken from the project.
 *
 * @param architectureId the catalog architecture to follow; empty to use the detected one
 * @param basePackage    the package the files go under; empty to infer it from the project
 * @param route          the REST path, without slashes; empty to derive it from the entity
 */
public record CrudRequest(String entity, List<Field> fields, Optional<String> architectureId, Optional<String> basePackage,
                          Optional<String> route) {

    public CrudRequest {
        fields = List.copyOf(fields);
    }

    /** {@code type} as the user wrote it, such as {@code String} or {@code BigDecimal}. */
    public record Field(String name, String type) {
    }
}
