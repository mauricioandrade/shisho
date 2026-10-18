package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Dependency;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Boot web applications, recognized by their starter (Boot 4 renamed {@code web} to
 * {@code webmvc}), and their controllers.
 */
final class WebApplications {

    static final String SPRING_BOOT = "org.springframework.boot";
    private static final List<String> WEB_STARTERS = List.of(
            "spring-boot-starter-web", "spring-boot-starter-webmvc", "spring-boot-starter-webflux");

    private static final Set<String> CONTROLLERS = Set.of("Controller", "RestController");
    private static final Set<String> REST_CONTROLLER = Set.of("RestController");
    private static final Set<String> RESPONSE_BODY = Set.of("ResponseBody");

    private WebApplications() {
    }

    /**
     * A {@code @RestController}, or a {@code @Controller} with a method whose return is the response
     * body. A {@code @Controller} without one only renders views (Thymeleaf, JSP).
     */
    static boolean answersWithData(ClassUnit unit) {
        if (Annotations.has(unit.annotations(), REST_CONTROLLER)) {
            return true;
        }
        return Annotations.has(unit.annotations(), CONTROLLERS)
                && (Annotations.has(unit.annotations(), RESPONSE_BODY)
                        || unit.methods().stream().anyMatch(method -> Annotations.has(method.annotations(), RESPONSE_BODY)
                                || method.returnType().startsWith("ResponseEntity")));
    }

    /**
     * Whether every controller of the project only renders views. False without controllers: the
     * routes may be functional ({@code RouterFunction}), which the classes do not show.
     */
    static boolean rendersViewsOnly(List<ClassUnit> classes) {
        List<ClassUnit> controllers = classes.stream().filter(unit -> Annotations.has(unit.annotations(), CONTROLLERS)).toList();
        return !controllers.isEmpty() && controllers.stream().noneMatch(WebApplications::answersWithData);
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
