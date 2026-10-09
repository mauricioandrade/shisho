package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;

/** A Spring Boot web application whose build does not declare the actuator starter. */
final class MissingActuatorKata implements ProjectKata {

    private static final String ACTUATOR = "spring-boot-starter-actuator";

    @Override
    public String id() {
        return "missing-actuator";
    }

    @Override
    public Severity severity() {
        return Severity.LEVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.BASELINE;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        return project.build()
                .filter(WebApplications::isWebApplication)
                .filter(build -> !build.dependsOn(WebApplications.SPRING_BOOT, ACTUATOR))
                .map(build -> lesson(build.file(), 0))
                .stream()
                .toList();
    }
}
