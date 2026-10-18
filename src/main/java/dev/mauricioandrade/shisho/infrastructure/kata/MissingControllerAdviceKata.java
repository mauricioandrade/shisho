package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/**
 * A project with controllers that answer with data and no {@code @ControllerAdvice}, or with Jakarta
 * REST resources (Quarkus) and no {@code ExceptionMapper} nor {@code @ServerExceptionMapper}; the
 * lesson points at the first one. A {@code @Controller} that only renders views (Thymeleaf) is left
 * out: Spring Boot's {@code error} view already handles its errors, and ProblemDetail is for APIs.
 */
final class MissingControllerAdviceKata implements ProjectKata {

    private static final Set<String> REST_CONTROLLER = Set.of("RestController");
    private static final Set<String> CONTROLLER = Set.of("Controller");
    private static final Set<String> RESPONSE_BODY = Set.of("ResponseBody");
    private static final Set<String> ADVICE = Set.of("ControllerAdvice", "RestControllerAdvice");
    private static final String EXCEPTION_MAPPER = "ExceptionMapper";
    private static final Set<String> SERVER_EXCEPTION_MAPPER = Set.of("ServerExceptionMapper");

    @Override
    public String id() {
        return "missing-controller-advice";
    }

    @Override
    public Severity severity() {
        return Severity.MEDIA;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.BASELINE;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        if (project.classes().stream().anyMatch(MissingControllerAdviceKata::handlesErrors)) {
            return List.of();
        }
        return project.classes().stream()
                .filter(unit -> answersWithData(unit) || JakartaRest.isResource(unit))
                .findFirst()
                .map(controller -> answersWithData(controller)
                        ? lesson(controller.file(), controller.line(), controller.name())
                        : QuarkusApplications.lesson(this, controller.file(), controller.line(), controller.name()))
                .stream()
                .toList();
    }

    /** A {@code @RestController}, or a {@code @Controller} with a method whose return is the response body. */
    private static boolean answersWithData(ClassUnit unit) {
        if (Annotations.has(unit.annotations(), REST_CONTROLLER)) {
            return true;
        }
        return Annotations.has(unit.annotations(), CONTROLLER)
                && (Annotations.has(unit.annotations(), RESPONSE_BODY)
                        || unit.methods().stream().anyMatch(method -> Annotations.has(method.annotations(), RESPONSE_BODY)
                                || method.returnType().startsWith("ResponseEntity")));
    }

    private static boolean handlesErrors(ClassUnit unit) {
        return Annotations.has(unit.annotations(), ADVICE)
                || unit.supertypes().contains(EXCEPTION_MAPPER)
                || unit.methods().stream().anyMatch(method -> Annotations.has(method.annotations(), SERVER_EXCEPTION_MAPPER));
    }
}
