package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Replaces {@code @RequiredArgsConstructor}/{@code @AllArgsConstructor} with the constructor
 * Lombok would generate: public, fields in declaration order. Left alone: annotations with
 * options, classes with explicit constructors or other Lombok annotations that depend on the
 * generated one ({@code @Builder}, {@code @Value}, {@code @FieldDefaults}), and {@code @NonNull}
 * fields, whose null checks Lombok would generate too.
 */
final class LombokConstructorFixer implements Fixer {

    private static final String REQUIRED = "RequiredArgsConstructor";
    private static final String ALL = "AllArgsConstructor";
    private static final Set<String> DEPENDENT = Set.of("Builder", "SuperBuilder", "Value", "FieldDefaults");

    @Override
    public String kataId() {
        return "lombok-constructor";
    }

    @Override
    public Set<String> removedAnnotations() {
        return Set.of(REQUIRED, ALL);
    }

    @Override
    public Optional<Runnable> prepare(CompilationUnit unit, Lesson lesson, Analysis analysis) {
        String kind = (String) lesson.arguments().getFirst();
        for (ClassOrInterfaceDeclaration type : unit.findAll(ClassOrInterfaceDeclaration.class)) {
            Optional<AnnotationExpr> annotation = type.getAnnotations().stream()
                    .filter(candidate -> candidate.getName().getIdentifier().equals(kind) && Trees.startsAt(candidate, lesson.line()))
                    .findFirst();
            if (annotation.isPresent()) {
                return prepare(type, annotation.get(), kind);
            }
        }
        return Optional.empty();
    }

    private static Optional<Runnable> prepare(ClassOrInterfaceDeclaration type, AnnotationExpr annotation, String kind) {
        boolean dependent = type.getAnnotations().stream().anyMatch(other -> DEPENDENT.contains(other.getName().getIdentifier()));
        if (!(annotation instanceof MarkerAnnotationExpr) || !type.getConstructors().isEmpty() || dependent || type.isInterface()) {
            return Optional.empty();
        }
        List<FieldDeclaration> fields = type.getFields().stream()
                .filter(field -> !field.isStatic())
                .filter(field -> kind.equals(ALL) ? !(field.isFinal() && hasInitializer(field)) : field.isFinal() && !hasInitializer(field))
                .toList();
        if (fields.stream().anyMatch(field -> field.getAnnotationByName("NonNull").isPresent() || field.getVariables().size() > 1)) {
            return Optional.empty();
        }
        return Optional.of(() -> {
            annotation.remove();
            List<Type> types = fields.stream().map(FieldDeclaration::getElementType).toList();
            boolean exists = type.getConstructors().stream().anyMatch(constructor -> constructor.getParameters().stream()
                    .map(parameter -> parameter.getType().asString()).toList()
                    .equals(types.stream().map(Type::asString).toList()));
            if (!exists) {
                ConstructorDeclaration constructor = Constructors.addAfterFields(type, true);
                fields.forEach(field -> Constructors.inject(constructor, field.getElementType(), field.getVariable(0).getNameAsString()));
            }
        });
    }

    private static boolean hasInitializer(FieldDeclaration field) {
        return field.getVariables().stream().anyMatch(variable -> variable.getInitializer().isPresent());
    }
}
