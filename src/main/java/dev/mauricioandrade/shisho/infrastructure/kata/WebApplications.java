package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Dependency;

import java.util.List;
import java.util.Optional;

/** Spring Boot web applications, recognized by their starter (Boot 4 renamed {@code web} to {@code webmvc}). */
final class WebApplications {

    static final String SPRING_BOOT = "org.springframework.boot";
    private static final List<String> WEB_STARTERS = List.of(
            "spring-boot-starter-web", "spring-boot-starter-webmvc", "spring-boot-starter-webflux");

    private WebApplications() {
    }

    static boolean isWebApplication(BuildInfo build) {
        return webStarter(build).isPresent();
    }

    /** The web starter of the build or of one of its modules: where a missing web dependency goes. */
    static Optional<Dependency> webStarter(BuildInfo build) {
        return WEB_STARTERS.stream().map(starter -> new Dependency(SPRING_BOOT, starter))
                .filter(starter -> build.dependsOn(starter.groupId(), starter.artifactId()))
                .findFirst();
    }
}
