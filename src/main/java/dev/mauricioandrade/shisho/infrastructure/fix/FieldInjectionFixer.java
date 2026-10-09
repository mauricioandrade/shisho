package dev.mauricioandrade.shisho.infrastructure.fix;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import dev.mauricioandrade.shisho.domain.model.Analysis;
import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.Optional;
import java.util.Set;

/**
 * Moves an injected field to the constructor: drops {@code @Autowired}/{@code @Inject}, makes the
 * field {@code final} and assigns it from a new constructor parameter, in the single existing
 * constructor or in a new one placed after the fields. Left alone: {@code @Autowired(required = false)},
 * fields with other annotations (a {@code @Qualifier} would have to move too), classes with several
 * constructors or a Lombok constructor.
 */
final class FieldInjectionFixer implements Fixer {

    private static final Set<String> INJECTION = Set.of("Autowired", "Inject");
    private static final Set<String> LOMBOK_CONSTRUCTORS = Set.of("RequiredArgsConstructor", "AllArgsConstructor", "NoArgsConstructor");

    @Override
    public String kataId() {
        return "field-injection";
    }

    @Override
    public Set<String> removedAnnotations() {
        return INJECTION;
    }

    @Override
    public Optional<Runnable> prepare(CompilationUnit unit, Lesson lesson, Analysis analysis) {
        String name = (String) lesson.arguments().getFirst();
        Optional<FieldDeclaration> found = Trees.field(unit, name, lesson.line())
                .filter(field -> !field.isStatic() && field.getVariable(0).getInitializer().isEmpty())
                .filter(field -> field.getAnnotations().size() == 1 && field.getAnnotation(0) instanceof MarkerAnnotationExpr)
                .filter(field -> INJECTION.contains(field.getAnnotation(0).getName().getIdentifier()));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        FieldDeclaration field = found.get();
        Optional<ClassOrInterfaceDeclaration> owner = Trees.enclosingClass(field).filter(type -> canReceive(type, name));
        return owner.map(type -> () -> {
            AnnotationExpr injection = field.getAnnotation(0);
            injection.remove();
            field.addModifier(Modifier.Keyword.FINAL);
            ConstructorDeclaration constructor = type.getConstructors().isEmpty()
                    ? Constructors.addAfterFields(type, type.isPublic())
                    : type.getConstructors().getFirst();
            Constructors.inject(constructor, field.getElementType(), name);
        });
    }

    private static boolean canReceive(ClassOrInterfaceDeclaration type, String name) {
        if (type.getConstructors().size() > 1
                || type.getAnnotations().stream().anyMatch(annotation -> LOMBOK_CONSTRUCTORS.contains(annotation.getName().getIdentifier()))) {
            return false;
        }
        return type.getConstructors().stream().noneMatch(constructor ->
                Constructors.delegates(constructor)
                        || constructor.getParameters().stream().anyMatch(parameter -> parameter.getNameAsString().equals(name)));
    }
}
