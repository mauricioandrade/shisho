package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;

/**
 * A Spring Boot web application with no springdoc-openapi in the build. Only a suggestion:
 * not every API is public, and some document their contract another way.
 */
final class MissingSpringdocKata implements ProjectKata {

    private static final String SPRINGDOC = "org.springdoc";

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
        return project.build()
                .filter(WebApplications::isWebApplication)
                .filter(build -> build.dependencies().stream().noneMatch(dependency -> dependency.groupId().equals(SPRINGDOC)))
                .map(build -> lesson(build.file(), 0))
                .stream()
                .toList();
    }
}
