package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.application.usecase.ProjectLoader.LoadedProject;
import dev.mauricioandrade.shisho.domain.exception.ConfigException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.Baseline;
import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Concept;
import dev.mauricioandrade.shisho.domain.model.ConceptDetector;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectConfig;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.port.in.AnalyzeProject;
import dev.mauricioandrade.shisho.domain.port.in.ClassifyArchitecture;
import dev.mauricioandrade.shisho.domain.port.out.BaselineReader;
import dev.mauricioandrade.shisho.domain.port.out.BuildFileReader;
import dev.mauricioandrade.shisho.domain.port.out.ProjectConfigReader;
import dev.mauricioandrade.shisho.domain.port.out.SettingsReader;
import dev.mauricioandrade.shisho.domain.port.out.SourceParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Loads the project (see {@link ProjectLoader}), classifies the architecture and applies every enabled kata that version allows, with
 * the configured severity. A kata that suggests a newer Java feature still runs below its version,
 * as INFO suggestions for after the upgrade. Lessons are ordered by file, then line, then kata order.
 * With a baseline, the analysis keeps only the lessons it does not have; the score counts them all.
 */
public final class AnalyzeProjectService implements AnalyzeProject {

    private final ProjectLoader projectLoader;
    private final List<Kata> katas;
    private final ClassifyArchitecture classifyArchitecture;
    private final RankCalculator rankCalculator;
    private final ConceptFinder conceptFinder;
    private final BaselineReader baselineReader;

    /** Without concepts: the analysis the shoshin mode does not need. */
    public AnalyzeProjectService(
            BuildFileReader buildFileReader,
            ProjectConfigReader configReader,
            SourceParser sourceParser,
            List<Kata> katas,
            ClassifyArchitecture classifyArchitecture,
            RankCalculator rankCalculator) {
        this(buildFileReader, configReader, sourceParser, katas, classifyArchitecture, rankCalculator, List.of(), List.of());
    }

    public AnalyzeProjectService(
            BuildFileReader buildFileReader,
            ProjectConfigReader configReader,
            SourceParser sourceParser,
            List<Kata> katas,
            ClassifyArchitecture classifyArchitecture,
            RankCalculator rankCalculator,
            List<Concept> concepts,
            List<ConceptDetector> conceptDetectors) {
        this(buildFileReader, configReader, sourceParser, katas, classifyArchitecture, rankCalculator, concepts, conceptDetectors,
                _ -> Optional.empty(), _ -> List.of());
    }

    public AnalyzeProjectService(
            BuildFileReader buildFileReader,
            ProjectConfigReader configReader,
            SourceParser sourceParser,
            List<Kata> katas,
            ClassifyArchitecture classifyArchitecture,
            RankCalculator rankCalculator,
            List<Concept> concepts,
            List<ConceptDetector> conceptDetectors,
            BaselineReader baselineReader,
            SettingsReader settingsReader) {
        this.projectLoader = new ProjectLoader(buildFileReader, configReader, sourceParser, settingsReader);
        this.katas = List.copyOf(katas);
        this.classifyArchitecture = classifyArchitecture;
        this.rankCalculator = rankCalculator;
        this.conceptFinder = new ConceptFinder(concepts, conceptDetectors);
        this.baselineReader = baselineReader;
    }

    @Override
    public Analysis analyze(Path projectRoot) {
        LoadedProject loaded = projectLoader.load(projectRoot, this::checkKatasExist);
        ProjectConfig config = loaded.config();
        ProjectModel project = loaded.model();
        JavaVersion javaVersion = project.javaVersion();
        List<ClassUnit> classes = project.classes();
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
        Optional<Baseline.Comparison> baseline = baselineReader.read(projectRoot).map(recorded -> recorded.compare(lessons));
        return new Analysis(
                projectRoot,
                projectName(projectRoot), javaVersion, loaded.javaVersionDeclared(), project.build(), architecture,
                baseline.map(Baseline.Comparison::fresh).orElse(lessons),
                loaded.unparseable(), score, Rank.forScore(score), conceptFinder.find(project), baseline);
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
