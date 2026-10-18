package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.AvoidedDependency;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;

/** A dependency in the build that the catalog advises against ({@code catalog/dependencies}), one lesson per coordinate. */
final class AvoidedDependencyKata implements ProjectKata {

    private final List<AvoidedDependency> avoided;

    AvoidedDependencyKata(List<AvoidedDependency> avoided) {
        this.avoided = List.copyOf(avoided);
    }

    @Override
    public String id() {
        return "avoided-dependency";
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
        return project.build().stream().flatMap(build -> avoided.stream().flatMap(dependency -> dependency.coordinates().stream()
                        .filter(coordinates -> declares(build, coordinates))
                        .map(coordinates -> lesson(build.fileDeclaring(coordinates), 0, dependency.name(),
                                coordinates.groupId() + ":" + coordinates.artifactId(),
                                dependency.summary(), dependency.alternative()))))
                .toList();
    }

    private static boolean declares(BuildInfo build, Dependency coordinates) {
        return build.dependsOn(coordinates.groupId(), coordinates.artifactId());
    }
}
