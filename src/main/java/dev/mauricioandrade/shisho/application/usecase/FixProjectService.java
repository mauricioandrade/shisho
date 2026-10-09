package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.FixPlan;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.domain.port.in.FixProject;
import dev.mauricioandrade.shisho.domain.port.out.SourceFixer;

import java.nio.file.Path;

/** The same analysis as {@code analyze}, then the corrections the fixer can make. */
public final class FixProjectService implements FixProject {

    private final AnalyzeProject analyzeProject;
    private final SourceFixer sourceFixer;

    public FixProjectService(AnalyzeProject analyzeProject, SourceFixer sourceFixer) {
        this.analyzeProject = analyzeProject;
        this.sourceFixer = sourceFixer;
    }

    @Override
    public FixPlan plan(Path projectRoot) {
        Analysis analysis = analyzeProject.analyze(projectRoot);
        return new FixPlan(analysis, sourceFixer.fix(analysis));
    }
}
