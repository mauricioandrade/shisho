package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.exception.AnalysisException;
import dev.mauricioandrade.shisho.domain.exception.ConfigException;
import dev.mauricioandrade.shisho.domain.model.Analysis;
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
                return JavaVersion.BASELINE;
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

        assertThat(parser.languageLevels).containsExactly(JavaVersion.BASELINE);
        assertThat(analysis.javaVersion()).isEqualTo(JavaVersion.BASELINE);
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
    void refusesUnknownKatasInTheConfiguration() {
        ProjectConfig config = new ProjectConfig(Map.of("feild-injection", new KataSetting(false, Optional.empty())));

        assertThatThrownBy(() -> service(version(21), config, kata("leve", Severity.LEVE, 21), kata("grave", Severity.GRAVE, 21)).analyze(ROOT))
                .isInstanceOfSatisfying(ConfigException.class, e -> {
                    assertThat(e.messageKey()).isEqualTo("config.error.unknown-kata");
                    assertThat(e.arguments()).containsExactly("feild-injection", "grave, leve");
                });
        assertThat(parser.languageLevels).isEmpty();
    }

    @Test
    void refusesProjectsBelowTheBaseline() {
        assertThatThrownBy(() -> service(version(17)).analyze(ROOT))
                .isInstanceOfSatisfying(AnalysisException.class, e -> {
                    assertThat(e.messageKey()).isEqualTo("analysis.error.unsupported-java-version");
                    assertThat(e.arguments()).containsExactly(new JavaVersion(17), JavaVersion.BASELINE);
                });
        assertThat(parser.languageLevels).isEmpty();
    }

    private AnalyzeProjectService service(BuildFileReader buildFileReader, Kata... katas) {
        return service(buildFileReader, ProjectConfig.DEFAULT, katas);
    }

    private AnalyzeProjectService service(BuildFileReader buildFileReader, ProjectConfig config, Kata... katas) {
        return new AnalyzeProjectService(
                buildFileReader, root -> config, parser, List.of(katas), project -> Optional.empty(), new RankCalculator());
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
