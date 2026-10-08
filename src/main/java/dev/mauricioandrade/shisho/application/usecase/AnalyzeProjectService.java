package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.exception.AnalysisException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.domain.port.out.BuildFileReader;
import dev.mauricioandrade.shisho.domain.port.out.SourceParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Reads the build, parses the sources at the project's Java version and applies every kata that
 * version allows. Lessons are ordered by file, then line, then kata order.
 */
public final class AnalyzeProjectService implements AnalyzeProject {

    private final BuildFileReader buildFileReader;
    private final SourceParser sourceParser;
    private final List<Kata> katas;
    private final RankCalculator rankCalculator;

    public AnalyzeProjectService(
            BuildFileReader buildFileReader, SourceParser sourceParser, List<Kata> katas, RankCalculator rankCalculator) {
        this.buildFileReader = buildFileReader;
        this.sourceParser = sourceParser;
        this.katas = List.copyOf(katas);
        this.rankCalculator = rankCalculator;
    }

    @Override
    public Analysis analyze(Path projectRoot) {
        Optional<JavaVersion> declaredVersion = buildFileReader.read(projectRoot).flatMap(BuildInfo::javaVersion);
        JavaVersion javaVersion = declaredVersion.orElse(JavaVersion.BASELINE);
        if (!javaVersion.isAtLeast(JavaVersion.BASELINE)) {
            throw new AnalysisException("analysis.error.unsupported-java-version", javaVersion, JavaVersion.BASELINE);
        }
        List<Kata> applicable = katas.stream().filter(kata -> javaVersion.isAtLeast(kata.minJavaVersion())).toList();

        List<Lesson> lessons = new ArrayList<>();
        List<SourceFile.Unparseable> unparseable = new ArrayList<>();
        for (SourceFile file : sourceParser.parse(projectRoot, javaVersion)) {
            switch (file) {
                case SourceFile.Parsed parsed -> parsed.classes().forEach(unit ->
                        applicable.forEach(kata -> lessons.addAll(kata.evaluate(unit))));
                case SourceFile.Unparseable failure -> unparseable.add(failure);
            }
        }
        lessons.sort(Comparator.comparing((Lesson lesson) -> lesson.file().toString()).thenComparingInt(Lesson::line));

        int score = rankCalculator.score(lessons);
        return new Analysis(
                projectName(projectRoot), javaVersion, declaredVersion.isPresent(), lessons, unparseable, score, Rank.forScore(score));
    }

    private static String projectName(Path projectRoot) {
        Path name = projectRoot.toAbsolutePath().normalize().getFileName();
        return name == null ? projectRoot.toAbsolutePath().toString() : name.toString();
    }
}
