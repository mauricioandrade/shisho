package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A Spring Boot web application whose build does not declare the actuator starter, or a Quarkus
 * one without {@code quarkus-smallrye-health}. The lesson goes to the build that declares the web
 * server, the module that is the application.
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
                    Optional<Dependency> starter = WebApplications.webStarter(build);
                    if (starter.isPresent() && !build.dependsOn(WebApplications.SPRING_BOOT, ACTUATOR)) {
                        return Stream.of(lesson(build.fileDeclaring(starter.get()), 0));
                    }
                    Optional<Dependency> server = QuarkusApplications.restServer(build);
                    if (server.isPresent() && !build.dependsOn(QuarkusApplications.QUARKUS, SMALLRYE_HEALTH)) {
                        return Stream.of(QuarkusApplications.lesson(this, build.fileDeclaring(server.get()), 0));
                    }
                    return Stream.empty();
                })
                .toList();
    }
}
