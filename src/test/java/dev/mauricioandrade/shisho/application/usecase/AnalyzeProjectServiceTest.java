package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.exception.AnalysisException;
import dev.mauricioandrade.shisho.domain.exception.ConfigException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.Baseline;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectConfig;
import dev.mauricioandrade.shisho.domain.model.ProjectConfig.KataSetting;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Rank;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import dev.mauricioandrade.shisho.domain.port.in.ClassifyArchitecture;
import dev.mauricioandrade.shisho.domain.port.out.BuildFileReader;
import dev.mauricioandrade.shisho.domain.port.out.SourceParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class AnalyzeProjectServiceTest {

    private static final Path ROOT = Path.of("/projetos/loja");
    private static final ClassUnit B = unit("B.java", 1);
    private static final ClassUnit A = unit("A.java", 5);
    private static final ArchitectureProfile LAYERED = new ArchitectureProfile("layered", "Layered", Family.SIMPLE,
            new Signature(List.of("controller"), List.of(), 1), List.of(), new Cost(new ClassRange(4, 6), Complexity.LOW),
            List.of(), List.of(), List.of(), List.of(), Optional.empty(), List.of(), "");

    private final RecordingParser parser = new RecordingParser(List.of(
            new SourceFile.Parsed(Path.of("B.java"), List.of(B)),
            new SourceFile.Unparseable(Path.of("C.java"), 3, "boom"),
            new SourceFile.Parsed(Path.of("A.java"), List.of(A))));

    @Test
    void appliesKatasAndOrdersLessonsByFileThenLineThenKata() {
        Kata grave = kata("grave", Severity.GRAVE, 21, 7, 2);
        Kata leve = kata("leve", Severity.LEVE, 21, 2);

        Analysis analysis = service(version(21), grave, leve).analyze(ROOT);

        assertThat(analysis.lessons())
                .extracting(lesson -> lesson.file().toString(), Lesson::line, Lesson::kataId)
                .containsExactly(
                        tuple("A.java", 2, "grave"),
                        tuple("A.java", 2, "leve"),
                        tuple("A.java", 7, "grave"),
                        tuple("B.java", 2, "grave"),
                        tuple("B.java", 2, "leve"),
                        tuple("B.java", 7, "grave"));
        assertThat(analysis.score()).isEqualTo(100 - 4 * 10 - 2);
        assertThat(analysis.rank()).isEqualTo(Rank.KYU_4);
        assertThat(analysis.unparseableFiles()).extracting(SourceFile::file).containsExactly(Path.of("C.java"));
        assertThat(analysis.projectName()).isEqualTo("loja");
    }

    @Test
    void keepsOnlyTheLessonsTheBaselineDoesNotHaveButScoresThemAll() {
        Kata grave = kata("grave", Severity.GRAVE, 21, 7);
        Lesson knownInA = new Lesson("grave", Severity.GRAVE, Path.of("A.java"), 99, "kata.grave.lesson", List.of());
        AnalyzeProjectService service = new AnalyzeProjectService(version(21), root -> ProjectConfig.DEFAULT, parser, List.of(grave),
                (project, context) -> Optional.empty(), new RankCalculator(), List.of(), List.of(),
                root -> Optional.of(Baseline.of(List.of(knownInA))), root -> List.of());

        Analysis analysis = service.analyze(ROOT);

        assertThat(analysis.lessons()).extracting(lesson -> lesson.file().toString()).containsExactly("B.java");
        assertThat(analysis.baseline()).hasValueSatisfying(baseline ->
                assertThat(baseline.known()).extracting(lesson -> lesson.file().toString(), Lesson::line).containsExactly(tuple("A.java", 7)));
        assertThat(analysis.score()).isEqualTo(100 - 2 * 10);
        assertThat(analysis.allLessons()).hasSize(2);
    }

    @Test
    void givesProjectKatasTheWholeProjectOnce() {
        List<ProjectModel> seen = new ArrayList<>();
        ProjectKata projectKata = new ProjectKata() {
            @Override
            public String id() {
                return "project";
            }

            @Override
            public Severity severity() {
                return Severity.MEDIA;
            }

            @Override
            public JavaVersion minJavaVersion() {
                return JavaVersion.MINIMUM;
            }

            @Override
            public List<Lesson> evaluate(ProjectModel project) {
                seen.add(project);
                return List.of(lesson(Path.of("A.java"), 2));
            }
        };

        Analysis analysis = service(version(21), kata("grave", Severity.GRAVE, 21, 2), projectKata).analyze(ROOT);

        assertThat(seen).singleElement().satisfies(project -> {
            assertThat(project.classes()).containsExactly(B, A);
            assertThat(project.javaVersion()).isEqualTo(new JavaVersion(21));
            assertThat(project.build()).isPresent();
        });
        assertThat(analysis.lessons())
                .extracting(lesson -> lesson.file().toString(), Lesson::line, Lesson::kataId)
                .containsExactly(
                        tuple("A.java", 2, "grave"),
                        tuple("A.java", 2, "project"),
                        tuple("B.java", 2, "grave"));
    }

    @Test
    void parsesAtTheDeclaredVersion() {
        Analysis analysis = service(version(25)).analyze(ROOT);

        assertThat(parser.languageLevels).containsExactly(new JavaVersion(25));
        assertThat(analysis.javaVersion()).isEqualTo(new JavaVersion(25));
        assertThat(analysis.javaVersionDeclared()).isTrue();
    }

    @Test
    void assumesBaselineWhenTheVersionIsNotDeclared() {
        Analysis analysis = service(root -> Optional.empty()).analyze(ROOT);

        assertThat(parser.languageLevels).containsExactly(JavaVersion.ASSUMED);
        assertThat(analysis.javaVersion()).isEqualTo(JavaVersion.ASSUMED);
        assertThat(analysis.javaVersionDeclared()).isFalse();
    }

    @Test
    void skipsKatasAboveTheProjectVersion() {
        Kata future = kata("future", Severity.GRAVE, 25, 1);

        Analysis analysis = service(version(21), future).analyze(ROOT);

        assertThat(analysis.lessons()).isEmpty();
        assertThat(analysis.rank()).isEqualTo(Rank.DAN_1);
    }

    @Test
    void skipsKatasTurnedOffAndAppliesConfiguredSeverities() {
        ProjectConfig config = new ProjectConfig(Map.of(
                "grave", new KataSetting(true, Optional.of(Severity.INFO)),
                "leve", new KataSetting(false, Optional.empty())));

        Analysis analysis = service(version(21), config,
                kata("grave", Severity.GRAVE, 21, 7), kata("leve", Severity.LEVE, 21, 2)).analyze(ROOT);

        assertThat(analysis.lessons()).extracting(Lesson::kataId, Lesson::severity)
                .containsOnly(tuple("grave", Severity.INFO));
        assertThat(analysis.rank()).isEqualTo(Rank.DAN_1);
    }

    @Test
    void addsDependencyRuleViolationsAsConfigurableLessons() {
        Lesson violation = new Lesson("dependency-rule", Severity.MEDIA, Path.of("A.java"), 3, "architecture.dependency-rule.lesson", List.of());
        ArchitectureMatch match = new ArchitectureMatch(
                LAYERED, List.of("controller"), 50, List.of(violation), new Verdict.Adequate(false, List.of()));

        Analysis standard = service(version(21), ProjectConfig.DEFAULT, (project, context) -> Optional.of(match)).analyze(ROOT);
        assertThat(standard.architecture()).contains(match);
        assertThat(standard.lessons()).containsExactly(violation);

        ProjectConfig leve = new ProjectConfig(Map.of("dependency-rule", new KataSetting(true, Optional.of(Severity.LEVE))));
        assertThat(service(version(21), leve, (project, context) -> Optional.of(match)).analyze(ROOT).lessons())
                .extracting(Lesson::severity).containsExactly(Severity.LEVE);

        ProjectConfig off = new ProjectConfig(Map.of("dependency-rule", new KataSetting(false, Optional.empty())));
        assertThat(service(version(21), off, (project, context) -> Optional.of(match)).analyze(ROOT).lessons()).isEmpty();
    }

    @Test
    void refusesUnknownKatasInTheConfiguration() {
        ProjectConfig config = new ProjectConfig(Map.of("feild-injection", new KataSetting(false, Optional.empty())));

        assertThatThrownBy(() -> service(version(21), config, kata("leve", Severity.LEVE, 21), kata("grave", Severity.GRAVE, 21)).analyze(ROOT))
                .isInstanceOfSatisfying(ConfigException.class, e -> {
                    assertThat(e.messageKey()).isEqualTo("config.error.unknown-kata");
                    assertThat(e.arguments()).containsExactly("feild-injection", "dependency-rule, grave, leve");
                });
        assertThat(parser.languageLevels).isEmpty();
    }

    @Test
    void katasSuggestingANewerJavaFeatureBecomeUpgradeSuggestions() {
        Kata scoped = new ClassKata() {
            @Override
            public String id() {
                return "scoped";
            }

            @Override
            public Severity severity() {
                return Severity.LEVE;
            }

            @Override
            public JavaVersion minJavaVersion() {
                return new JavaVersion(25);
            }

            @Override
            public Optional<String> javaFeature() {
                return Optional.of("scoped-values");
            }

            @Override
            public List<Lesson> evaluate(ClassUnit unit) {
                return List.of(lesson(unit, 3));
            }
        };

        Analysis on21 = service(version(21), scoped, kata("future", Severity.GRAVE, 25, 1)).analyze(ROOT);
        assertThat(on21.lessons()).extracting(Lesson::kataId, Lesson::severity, Lesson::afterUpgradeTo).containsExactly(
                tuple("scoped", Severity.INFO, Optional.of(new JavaVersion(25))),
                tuple("scoped", Severity.INFO, Optional.of(new JavaVersion(25))));
        assertThat(on21.rank()).isEqualTo(Rank.DAN_1);

        parser.languageLevels.clear();
        Analysis on25 = service(version(25), scoped).analyze(ROOT);
        assertThat(on25.lessons()).extracting(Lesson::severity, Lesson::afterUpgradeTo)
                .containsOnly(tuple(Severity.LEVE, Optional.empty()));
    }

    @Test
    void refusesProjectsBelowTheMinimum() {
        assertThatThrownBy(() -> service(version(7)).analyze(ROOT))
                .isInstanceOfSatisfying(AnalysisException.class, e -> {
                    assertThat(e.messageKey()).isEqualTo("analysis.error.unsupported-java-version");
                    assertThat(e.arguments()).containsExactly(new JavaVersion(7), JavaVersion.MINIMUM);
                });
        assertThat(parser.languageLevels).isEmpty();
    }

    private AnalyzeProjectService service(BuildFileReader buildFileReader, Kata... katas) {
        return service(buildFileReader, ProjectConfig.DEFAULT, katas);
    }

    private AnalyzeProjectService service(BuildFileReader buildFileReader, ProjectConfig config, Kata... katas) {
        return service(buildFileReader, config, (project, context) -> Optional.empty(), katas);
    }

    private AnalyzeProjectService service(
            BuildFileReader buildFileReader, ProjectConfig config, ClassifyArchitecture classifier, Kata... katas) {
        return new AnalyzeProjectService(buildFileReader, root -> config, parser, List.of(katas), classifier, new RankCalculator());
    }

    private static BuildFileReader version(int feature) {
        return root -> Optional.of(new BuildInfo(Path.of("pom.xml"), Optional.of(new JavaVersion(feature)), List.of()));
    }

    private static ClassUnit unit(String file, int line) {
        return new ClassUnit(Path.of(file), "", file, ClassUnit.Kind.CLASS, line,
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** A kata that reports one lesson per given line in every class. */
    private static Kata kata(String id, Severity severity, int minJavaVersion, int... lines) {
        return new ClassKata() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Severity severity() {
                return severity;
            }

            @Override
            public JavaVersion minJavaVersion() {
                return new JavaVersion(minJavaVersion);
            }

            @Override
            public List<Lesson> evaluate(ClassUnit unit) {
                return Arrays.stream(lines).mapToObj(line -> lesson(unit, line)).toList();
            }
        };
    }

    private static final class RecordingParser implements SourceParser {

        private final List<SourceFile> files;
        private final List<JavaVersion> languageLevels = new ArrayList<>();

        private RecordingParser(List<SourceFile> files) {
            this.files = files;
        }

        @Override
        public List<SourceFile> parse(Path projectRoot, JavaVersion languageLevel) {
            languageLevels.add(languageLevel);
            return files;
        }
    }
}
