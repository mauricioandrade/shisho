package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A method named like a read (in English or Portuguese) running in a read-write Spring
 * transaction, declared on the method or inherited from the class. JTA's
 * {@code jakarta.transaction.Transactional} has no {@code readOnly}, so classes using it are skipped.
 */
final class TransactionalReadOnlyKata implements ClassKata {

    private static final Set<String> TRANSACTIONAL = Set.of("Transactional");
    private static final Set<String> JTA = Set.of("jakarta.transaction.Transactional", "javax.transaction.Transactional");
    private static final Pattern READ_METHOD = Pattern.compile(
            "(find|get|list|search|count|exists|fetch|load"
                    + "|buscar|listar|contar|obter|consultar|pesquisar|carregar|existe)(\\p{Lu}.*)?");

    @Override
    public String id() {
        return "transactional-read-only";
    }

    @Override
    public Severity severity() {
        return Severity.LEVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.BASELINE;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        if (unit.imports().stream().anyMatch(declaration -> JTA.contains(declaration.name()))) {
            return List.of();
        }
        Optional<Annotation> classTransaction = Annotations.find(unit.annotations(), TRANSACTIONAL);
        return unit.methods().stream()
                .filter(method -> READ_METHOD.matcher(method.name()).matches())
                .flatMap(method -> {
                    Optional<Annotation> own = Annotations.find(method.annotations(), TRANSACTIONAL);
                    return own.or(() -> classTransaction)
                            .filter(transaction -> !transaction.attribute("readOnly").orElse("false").equals("true"))
                            .map(_ -> lesson(unit, own.map(Annotation::line).orElse(method.line()), method.name()))
                            .stream();
                })
                .toList();
    }
}
