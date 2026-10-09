package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Parameter;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;

/** {@code @RequestBody} without {@code @Valid}/{@code @Validated}, for bodies that have something to validate. */
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
        return unit.methods().stream()
                .flatMap(method -> method.parameters().stream())
                .filter(parameter -> !Annotations.has(parameter.annotations(), VALIDATION))
                .filter(RequestBodyWithoutValidKata::hasStructuredBody)
                .flatMap(parameter -> Annotations.find(parameter.annotations(), REQUEST_BODY)
                        .map(annotation -> lesson(unit, annotation.line(), parameter.type()))
                        .stream())
                .toList();
    }

    private static boolean hasStructuredBody(Parameter parameter) {
        String type = parameter.type();
        return !type.equals("String") && !type.equals("byte[]") && !type.equals("Map") && !type.startsWith("Map<");
    }
}
