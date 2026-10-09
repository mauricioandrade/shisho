package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.Optional;

/**
 * Adds {@code @Valid} before {@code @RequestBody}, importing {@code jakarta.validation.Valid}
 * when needed. Only when the build declares Bean Validation: without it the import would not
 * compile, and the annotations of the DTO would still not be checked.
 */
final class RequestBodyWithoutValidFixer implements Fixer {

    private static final String VALID = "jakarta.validation.Valid";

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
                        annotation.getName().getIdentifier().equals("RequestBody") && Trees.startsAt(annotation, lesson.line())))
                .findFirst()
                .map(parameter -> () -> {
                    parameter.getAnnotations().addFirst(new MarkerAnnotationExpr("Valid"));
                    if (!importsValid(unit)) {
                        unit.addImport(VALID);
                    }
                });
    }

    private static boolean hasBeanValidation(BuildInfo build) {
        return build.dependsOn("org.springframework.boot", "spring-boot-starter-validation")
                || build.dependencies().stream().anyMatch(dependency ->
                        dependency.groupId().equals("jakarta.validation") || dependency.groupId().equals("org.hibernate.validator"));
    }

    private static boolean importsValid(CompilationUnit unit) {
        return unit.getImports().stream().anyMatch(declaration -> declaration.isAsterisk()
                ? declaration.getNameAsString().equals("jakarta.validation")
                : declaration.getName().getIdentifier().equals("Valid"));
    }
}
