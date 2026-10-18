package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.exception.AnalysisException;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.ProjectConfig;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.domain.port.out.BuildFileReader;
import dev.mauricioandrade.shisho.domain.port.out.ProjectConfigReader;
import dev.mauricioandrade.shisho.domain.port.out.SourceParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Reads the build and the project's configuration, then parses the sources at the project's Java
 * version: what every use case needs before looking at the code.
 */
final class ProjectLoader {

    private final BuildFileReader buildFileReader;
    private final ProjectConfigReader configReader;
    private final SourceParser sourceParser;

    ProjectLoader(BuildFileReader buildFileReader, ProjectConfigReader configReader, SourceParser sourceParser) {
        this.buildFileReader = buildFileReader;
        this.configReader = configReader;
        this.sourceParser = sourceParser;
    }

    /**
     * @param javaVersionDeclared false when the build did not state a version and {@link JavaVersion#ASSUMED} was assumed
     */
    record LoadedProject(ProjectModel model, ProjectConfig config, boolean javaVersionDeclared, List<SourceFile.Unparseable> unparseable) {

        LoadedProject {
            unparseable = List.copyOf(unparseable);
        }
    }

    /**
     * @param checkConfig validates the configuration before the sources are parsed, so a typo
     *                    fails fast
     * @throws AnalysisException if the project's Java version is below {@link JavaVersion#MINIMUM}
     */
    LoadedProject load(Path projectRoot, Consumer<ProjectConfig> checkConfig) {
        Optional<BuildInfo> build = buildFileReader.read(projectRoot);
        Optional<JavaVersion> declaredVersion = build.flatMap(BuildInfo::lowestJavaVersion);
        JavaVersion javaVersion = declaredVersion.orElse(JavaVersion.ASSUMED);
        if (!javaVersion.isAtLeast(JavaVersion.MINIMUM)) {
            throw new AnalysisException("analysis.error.unsupported-java-version", javaVersion, JavaVersion.MINIMUM);
        }
        ProjectConfig config = configReader.read(projectRoot);
        checkConfig.accept(config);

        List<ClassUnit> classes = new ArrayList<>();
        List<SourceFile.Unparseable> unparseable = new ArrayList<>();
        for (SourceFile file : sourceParser.parse(projectRoot, javaVersion)) {
            switch (file) {
                case SourceFile.Parsed parsed -> classes.addAll(parsed.classes());
                case SourceFile.Unparseable failure -> unparseable.add(failure);
            }
        }
        return new LoadedProject(new ProjectModel(javaVersion, build, classes), config, declaredVersion.isPresent(), unparseable);
    }
}
