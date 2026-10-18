package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.Baseline;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.domain.port.in.RecordBaseline;
import dev.mauricioandrade.shisho.domain.port.out.BaselineWriter;

import java.nio.file.Path;
import java.util.List;

/** Records the project's lessons, the ones an older baseline knew included, as its new baseline. */
public final class RecordBaselineService implements RecordBaseline {

    private final AnalyzeProject analyzeProject;
    private final BaselineWriter baselineWriter;

    public RecordBaselineService(AnalyzeProject analyzeProject, BaselineWriter baselineWriter) {
        this.analyzeProject = analyzeProject;
        this.baselineWriter = baselineWriter;
    }

    @Override
    public Recorded record(Path projectRoot) {
        List<Lesson> lessons = analyzeProject.analyze(projectRoot).allLessons();
        return new Recorded(baselineWriter.write(projectRoot, Baseline.of(lessons)), lessons);
    }
}
