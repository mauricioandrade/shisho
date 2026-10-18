package dev.mauricioandrade.shisho.domain.port.in;

import dev.mauricioandrade.shisho.domain.model.CrudPlan;
import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;

import java.nio.file.Path;

public interface GenerateCrud {

    /** The templates generate records and Spring Boot 3 / Quarkus 3 code, which needs Java 17. */
    JavaVersion MINIMUM_JAVA = new JavaVersion(17);

    /**
     * Plans the CRUD in the project's architecture, or the one requested; nothing is written.
     *
     * @throws dev.mauricioandrade.shisho.domain.exception.ScaffoldException if the request is
     *         invalid, the project is below {@link #MINIMUM_JAVA} or there is no architecture or
     *         base package to follow
     * @throws dev.mauricioandrade.shisho.domain.exception.ShishoException if the project cannot be read
     */
    CrudPlan plan(Path projectRoot, CrudRequest request);
}
