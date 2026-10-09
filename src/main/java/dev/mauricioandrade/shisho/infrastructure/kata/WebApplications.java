package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;

import java.util.List;

/** Spring Boot web applications, recognized by their starter (Boot 4 renamed {@code web} to {@code webmvc}). */
final class WebApplications {

    static final String SPRING_BOOT = "org.springframework.boot";
    private static final List<String> WEB_STARTERS = List.of(
            "spring-boot-starter-web", "spring-boot-starter-webmvc", "spring-boot-starter-webflux");

    private WebApplications() {
    }

    static boolean isWebApplication(BuildInfo build) {
        return WEB_STARTERS.stream().anyMatch(starter -> build.dependsOn(SPRING_BOOT, starter));
    }
}
