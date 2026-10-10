package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.GeneratedFile;
import dev.mauricioandrade.shisho.domain.model.Scaffold;

import java.nio.file.Path;
import java.util.List;

public interface ScaffoldRenderer {

    /**
     * Fills the scaffold's templates for the requested entity; nothing is written.
     *
     * @return one file per {@link Scaffold#files()}, in the same order
     * @throws dev.mauricioandrade.shisho.domain.exception.ScaffoldException if the entity, a
     *         field, a type or the package is not valid
     */
    List<GeneratedFile> render(Scaffold scaffold, String basePackage, CrudRequest request);

    /**
     * Where {@link #render} would put each file for an entity named {@code entity}, relative to the
     * project root; empty if the name could not be an entity.
     */
    List<Path> paths(Scaffold scaffold, String basePackage, String entity);
}
