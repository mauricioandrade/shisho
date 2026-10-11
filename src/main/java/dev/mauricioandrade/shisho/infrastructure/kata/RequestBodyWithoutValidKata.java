package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Parameter;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A request body without {@code @Valid}/{@code @Validated}, for bodies that have something to validate:
 * Spring's {@code @RequestBody}, or the body of a Jakarta REST resource method (Quarkus).
 */
final class RequestBodyWithoutValidKata implements ClassKata {

    private static final Set<String> REQUEST_BODY = Set.of("RequestBody");
    private static final Set<String> VALIDATION = Set.of("Valid", "Validated");

    @Override
    public String id() {
        return "request-body-without-valid";
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
    public List<Lesson> evaluate(ClassUnit unit) {
        Stream<Lesson> spring = unit.methods().stream()
                .flatMap(method -> method.parameters().stream())
                .filter(RequestBodyWithoutValidKata::lacksValidation)
                .flatMap(parameter -> Annotations.find(parameter.annotations(), REQUEST_BODY)
                        .map(annotation -> lesson(unit, annotation.line(), parameter.type()))
                        .stream());
        Stream<Lesson> jakartaRest = unit.methods().stream()
                .filter(JakartaRest::isResourceMethod)
                .flatMap(method -> JakartaRest.body(unit, method).stream())
                .filter(RequestBodyWithoutValidKata::lacksValidation)
                .map(parameter -> QuarkusApplications.lesson(this, unit.file(), parameter.line(), parameter.type()));
        return Stream.concat(spring, jakartaRest).toList();
    }

    private static boolean lacksValidation(Parameter parameter) {
        return !Annotations.has(parameter.annotations(), VALIDATION) && hasStructuredBody(parameter);
    }

    private static boolean hasStructuredBody(Parameter parameter) {
        String type = parameter.type();
        return !type.equals("String") && !type.equals("byte[]") && !type.equals("Map") && !type.startsWith("Map<");
    }
}
