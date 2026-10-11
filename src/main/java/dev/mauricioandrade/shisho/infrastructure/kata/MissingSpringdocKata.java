package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.stream.Stream;

/**
 * A Spring Boot web application with no springdoc-openapi in the build, or a Quarkus one without
 * {@code quarkus-smallrye-openapi}. Only a suggestion: not every API is public, and some document
 * their contract another way.
 */
final class MissingSpringdocKata implements ProjectKata {

    private static final String SPRINGDOC = "org.springdoc";
    private static final String SMALLRYE_OPENAPI = "quarkus-smallrye-openapi";

    @Override
    public String id() {
        return "missing-springdoc";
    }

    @Override
    public Severity severity() {
        return Severity.INFO;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.BASELINE;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        return project.build().stream()
                .flatMap(build -> {
                    if (WebApplications.isWebApplication(build)
                            && build.dependencies().stream().noneMatch(dependency -> dependency.groupId().equals(SPRINGDOC))) {
                        return Stream.of(lesson(build.file(), 0));
                    }
                    if (QuarkusApplications.isWebApplication(build) && !build.dependsOn(QuarkusApplications.QUARKUS, SMALLRYE_OPENAPI)) {
                        return Stream.of(QuarkusApplications.lesson(this, build.file(), 0));
                    }
                    return Stream.empty();
                })
                .toList();
    }
}
