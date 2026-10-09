package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.exception.AnalysisException;
import dev.mauricioandrade.shisho.domain.exception.ConfigException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectConfig;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.domain.port.in.ClassifyArchitecture;
import dev.mauricioandrade.shisho.domain.port.out.BuildFileReader;
import dev.mauricioandrade.shisho.domain.port.out.ProjectConfigReader;
import dev.mauricioandrade.shisho.domain.port.out.SourceParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Reads the build and the project's configuration, parses the sources at the project's Java
 * version, classifies the architecture and applies every enabled kata that version allows, with
 * the configured severity. A kata that suggests a newer Java feature still runs below its version,
 * as INFO suggestions for after the upgrade. Lessons are ordered by file, then line, then kata order.
 */
public final class AnalyzeProjectService implements AnalyzeProject {

    private final BuildFileReader buildFileReader;
    private final ProjectConfigReader configReader;
    private final SourceParser sourceParser;
    private final List<Kata> katas;
    private final ClassifyArchitecture classifyArchitecture;
    private final RankCalculator rankCalculator;

    public AnalyzeProjectService(
            BuildFileReader buildFileReader,
            ProjectConfigReader configReader,
            SourceParser sourceParser,
            List<Kata> katas,
            ClassifyArchitecture classifyArchitecture,
            RankCalculator rankCalculator) {
        this.buildFileReader = buildFileReader;
        this.configReader = configReader;
        this.sourceParser = sourceParser;
        this.katas = List.copyOf(katas);
        this.classifyArchitecture = classifyArchitecture;
        this.rankCalculator = rankCalculator;
    }

    @Override
    public Analysis analyze(Path projectRoot) {
        Optional<BuildInfo> build = buildFileReader.read(projectRoot);
        Optional<JavaVersion> declaredVersion = build.flatMap(BuildInfo::javaVersion);
        JavaVersion javaVersion = declaredVersion.orElse(JavaVersion.BASELINE);
        if (!javaVersion.isAtLeast(JavaVersion.BASELINE)) {
            throw new AnalysisException("analysis.error.unsupported-java-version", javaVersion, JavaVersion.BASELINE);
        }
        ProjectConfig config = configReader.read(projectRoot);
        checkKatasExist(config);

        List<ClassUnit> classes = new ArrayList<>();
        List<SourceFile.Unparseable> unparseable = new ArrayList<>();
        for (SourceFile file : sourceParser.parse(projectRoot, javaVersion)) {
            switch (file) {
                case SourceFile.Parsed parsed -> classes.addAll(parsed.classes());
                case SourceFile.Unparseable failure -> unparseable.add(failure);
            }
        }
        ProjectModel project = new ProjectModel(javaVersion, build, classes);
        Optional<ArchitectureMatch> architecture = classifyArchitecture.classify(project, config.context());

        List<Lesson> lessons = new ArrayList<>();
        for (Kata kata : katas) {
            boolean available = javaVersion.isAtLeast(kata.minJavaVersion());
            if (!config.isEnabled(kata.id()) || !available && kata.javaFeature().isEmpty()) {
                continue;
            }
            List<Lesson> found = switch (kata) {
                case ClassKata classKata -> classes.stream().flatMap(unit -> classKata.evaluate(unit).stream()).toList();
                case ProjectKata projectKata -> projectKata.evaluate(project);
            };
            Severity severity = config.severity(kata.id(), kata.severity());
            found.forEach(lesson -> lessons.add(available
                    ? lesson.withSeverity(severity)
                    : lesson.afterUpgradeTo(kata.minJavaVersion())));
        }
        if (config.isEnabled(ArchitectureMatch.DEPENDENCY_RULE)) {
            Severity severity = config.severity(ArchitectureMatch.DEPENDENCY_RULE, ArchitectureMatch.DEPENDENCY_RULE_SEVERITY);
            architecture.stream().flatMap(match -> match.violations().stream())
                    .forEach(lesson -> lessons.add(lesson.withSeverity(severity)));
        }
        // stable sort: lessons on the same line keep the kata order
        lessons.sort(Comparator.comparing((Lesson lesson) -> lesson.file().toString()).thenComparingInt(Lesson::line));

        int score = rankCalculator.score(lessons);
        return new Analysis(
                projectRoot,
                projectName(projectRoot), javaVersion, declaredVersion.isPresent(), architecture, lessons, unparseable, score, Rank.forScore(score));
    }

    /** A typo in a kata id would silently configure nothing, so it is an error. */
    private void checkKatasExist(ProjectConfig config) {
        List<String> known = Stream.concat(katas.stream().map(Kata::id), Stream.of(ArchitectureMatch.DEPENDENCY_RULE))
                .sorted()
                .toList();
        config.katas().keySet().stream().filter(id -> !known.contains(id)).sorted().findFirst().ifPresent(unknown -> {
            throw new ConfigException("config.error.unknown-kata", unknown, String.join(", ", known));
        });
    }

    private static String projectName(Path projectRoot) {
        Path name = projectRoot.toAbsolutePath().normalize().getFileName();
        return name == null ? projectRoot.toAbsolutePath().toString() : name.toString();
    }
}
