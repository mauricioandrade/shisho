package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.stream.Stream;

/**
 * A Spring Boot web application whose build does not declare the actuator starter, or a Quarkus
 * one without {@code quarkus-smallrye-health}.
 */
final class MissingActuatorKata implements ProjectKata {

    private static final String ACTUATOR = "spring-boot-starter-actuator";
    private static final String SMALLRYE_HEALTH = "quarkus-smallrye-health";

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
        return project.build().stream()
                .flatMap(build -> {
                    if (WebApplications.isWebApplication(build) && !build.dependsOn(WebApplications.SPRING_BOOT, ACTUATOR)) {
                        return Stream.of(lesson(build.file(), 0));
                    }
                    if (QuarkusApplications.isWebApplication(build) && !build.dependsOn(QuarkusApplications.QUARKUS, SMALLRYE_HEALTH)) {
                        return Stream.of(QuarkusApplications.lesson(this, build.file(), 0));
                    }
                    return Stream.empty();
                })
                .toList();
    }
}
