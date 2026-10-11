package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.Optional;

/** Drops {@code private} from an injected field, leaving it package-private for ArC. */
final class QuarkusPrivateInjectionFixer implements Fixer {

    @Override
    public String kataId() {
        return "quarkus-private-injection";
    }

    @Override
    public Optional<Runnable> prepare(CompilationUnit unit, Lesson lesson, Analysis analysis) {
        String name = (String) lesson.arguments().getFirst();
        return Trees.field(unit, name, lesson.line())
                .filter(field -> field.isPrivate())
                .map(field -> () -> field.removeModifier(Modifier.Keyword.PRIVATE));
    }
}
