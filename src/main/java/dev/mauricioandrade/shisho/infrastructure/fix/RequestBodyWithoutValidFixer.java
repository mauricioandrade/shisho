package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.Optional;

/**
 * Adds {@code @Valid} before the request body ({@code @RequestBody} in Spring, the unannotated
 * parameter in Jakarta REST), importing the {@code @Valid} the lesson names
 * ({@code jakarta.validation.Valid}, or {@code javax.validation.Valid} on Spring Boot 2) when needed. Only when the build declares Bean Validation: without it the import would not
 * compile, and the annotations of the DTO would still not be checked.
 */
final class RequestBodyWithoutValidFixer implements Fixer {

    @Override
    public String kataId() {
        return "request-body-without-valid";
    }

    @Override
    public Optional<Runnable> prepare(CompilationUnit unit, Lesson lesson, Analysis analysis) {
        if (analysis.build().filter(RequestBodyWithoutValidFixer::hasBeanValidation).isEmpty()) {
            return Optional.empty();
        }
        String type = (String) lesson.arguments().getFirst();
        return unit.findAll(Parameter.class).stream()
                .filter(parameter -> parameter.getTypeAsString().equals(type))
                .filter(parameter -> parameter.getAnnotations().stream().anyMatch(annotation ->
                        annotation.getName().getIdentifier().equals("RequestBody") && Trees.startsAt(annotation, lesson.line()))
                        || parameter.getAnnotations().isEmpty() && Trees.startsAt(parameter, lesson.line()))
                .findFirst()
                .map(parameter -> () -> {
                    parameter.getAnnotations().addFirst(new MarkerAnnotationExpr("Valid"));
                    String valid = (String) lesson.arguments().get(1);
                    if (!importsValid(unit, valid)) {
                        unit.addImport(valid);
                    }
                });
    }

    private static boolean hasBeanValidation(BuildInfo build) {
        return build.dependsOn("org.springframework.boot", "spring-boot-starter-validation")
                || build.dependsOn("io.quarkus", "quarkus-hibernate-validator")
                || build.allDependencies().stream().anyMatch(dependency ->
                        dependency.groupId().equals("jakarta.validation") || dependency.groupId().equals("javax.validation")
                                || dependency.groupId().equals("org.hibernate.validator"));
    }

    private static boolean importsValid(CompilationUnit unit, String valid) {
        String validationPackage = valid.substring(0, valid.lastIndexOf('.'));
        return unit.getImports().stream().anyMatch(declaration -> declaration.isAsterisk()
                ? declaration.getNameAsString().equals(validationPackage)
                : declaration.getName().getIdentifier().equals("Valid"));
    }
}
