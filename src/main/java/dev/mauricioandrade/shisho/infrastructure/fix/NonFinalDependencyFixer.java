package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.Optional;

/**
 * Makes the dependency {@code final}, when that still compiles: no assignment outside the
 * constructors, and every constructor assigns it (or delegates with {@code this(...)}).
 */
final class NonFinalDependencyFixer implements Fixer {

    @Override
    public String kataId() {
        return "non-final-dependency";
    }

    @Override
    public Optional<Runnable> prepare(CompilationUnit unit, Lesson lesson, Analysis analysis) {
        String name = (String) lesson.arguments().getFirst();
        return Trees.field(unit, name, lesson.line())
                .filter(field -> field.getVariable(0).getInitializer().isEmpty())
                .filter(field -> Trees.enclosingClass(field).filter(type -> canBeFinal(type, name)).isPresent())
                .map(field -> () -> field.addModifier(Modifier.Keyword.FINAL));
    }

    private static boolean canBeFinal(ClassOrInterfaceDeclaration type, String name) {
        if (Trees.assignedOutsideConstructors(type, name)) {
            return false;
        }
        if (type.getConstructors().isEmpty()) {
            return type.getAnnotationByName("AllArgsConstructor").isPresent();
        }
        return type.getConstructors().stream().allMatch(constructor -> delegates(constructor) || Trees.assigns(constructor, name));
    }

    private static boolean delegates(ConstructorDeclaration constructor) {
        return constructor.getBody().getStatements().stream()
                .anyMatch(statement -> statement instanceof ExplicitConstructorInvocationStmt call && call.isThis());
    }
}
