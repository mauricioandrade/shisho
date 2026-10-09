package dev.mauricioandrade.shisho.domain.port.in;

import dev.mauricioandrade.shisho.domain.model.CrudPlan;
import dev.mauricioandrade.shisho.domain.model.CrudRequest;

import java.nio.file.Path;

public interface GenerateCrud {

    /**
     * Plans the CRUD in the project's architecture, or the one requested; nothing is written.
     *
     * @throws dev.mauricioandrade.shisho.domain.exception.ScaffoldException if the request is
     *         invalid or there is no architecture or base package to follow
     * @throws dev.mauricioandrade.shisho.domain.exception.ShishoException if the project cannot be read
     */
    CrudPlan plan(Path projectRoot, CrudRequest request);
}
