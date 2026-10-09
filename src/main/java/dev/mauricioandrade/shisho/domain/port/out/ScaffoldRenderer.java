package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.GeneratedFile;
import dev.mauricioandrade.shisho.domain.model.Scaffold;

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
}
