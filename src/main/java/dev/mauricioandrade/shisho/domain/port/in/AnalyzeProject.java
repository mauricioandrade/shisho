package dev.mauricioandrade.shisho.domain.port.in;

import dev.mauricioandrade.shisho.domain.model.Analysis;

import java.nio.file.Path;

public interface AnalyzeProject {

    /**
     * @throws dev.mauricioandrade.shisho.domain.exception.ShishoException if the project cannot
     *         be analyzed (unreadable, unsupported Java version, broken build file)
     */
    Analysis analyze(Path projectRoot);
}
