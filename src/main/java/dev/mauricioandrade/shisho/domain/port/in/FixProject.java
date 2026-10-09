package dev.mauricioandrade.shisho.domain.port.in;

import dev.mauricioandrade.shisho.domain.model.FixPlan;

import java.nio.file.Path;

public interface FixProject {

    /** Analyzes the project and plans the corrections; nothing is written. */
    FixPlan plan(Path projectRoot);
}
