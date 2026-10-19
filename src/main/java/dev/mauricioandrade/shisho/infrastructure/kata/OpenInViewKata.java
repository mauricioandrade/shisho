package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.domain.model.SettingsFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * A Spring Boot module with Spring Data JPA whose settings never say {@code spring.jpa.open-in-view}:
 * the default is {@code true}. Writing either value is a choice, and the kata accepts it. The
 * modules nested in it inherit the dependency, so their settings count too. A module that imports
 * its settings from a config server is left alone, since the setting may be there. The lesson
 * goes to the module's {@code application.properties} or {@code .yml}, or its build.
 */
final class OpenInViewKata implements ProjectKata {

    private static final Dependency DATA_JPA = new Dependency(WebApplications.SPRING_BOOT, "spring-boot-starter-data-jpa");
    private static final String OPEN_IN_VIEW = "spring.jpa.open-in-view";
    private static final String CONFIG_IMPORT = "spring.config.import";
    private static final String CONFIG_SERVER = "configserver:";
    private static final Set<String> MAIN_FILES = Set.of("application.properties", "application.yml", "application.yaml");

    @Override
    public String id() {
        return "open-in-view";
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
        return project.build().stream()
                .flatMap(BuildInfo::withModules)
                .filter(build -> build.dependencies().contains(DATA_JPA))
                .flatMap(build -> evaluate(build, inside(project, directory(build))).stream())
                .toList();
    }

    private Optional<Lesson> evaluate(BuildInfo build, List<SettingsFile> files) {
        boolean decided = files.stream().flatMap(file -> file.settings().stream()).anyMatch(setting -> {
            String key = ApplicationSettings.canonical(setting.key());
            return key.equals(ApplicationSettings.canonical(OPEN_IN_VIEW))
                    || key.equals(ApplicationSettings.canonical(CONFIG_IMPORT)) && setting.value().contains(CONFIG_SERVER);
        });
        if (decided) {
            return Optional.empty();
        }
        Path file = files.stream().map(SettingsFile::file)
                .filter(path -> MAIN_FILES.contains(path.getFileName().toString()))
                .findFirst()
                .orElse(build.file());
        return Optional.of(lesson(file, 0));
    }

    /** The settings files in the module's directory, the ones of modules nested in it included. */
    private static List<SettingsFile> inside(ProjectModel project, Path module) {
        return project.settings().stream()
                .filter(file -> module.toString().isEmpty() || file.file().startsWith(module))
                .toList();
    }

    private static Path directory(BuildInfo build) {
        return Optional.ofNullable(build.file().getParent()).orElse(Path.of(""));
    }
}
