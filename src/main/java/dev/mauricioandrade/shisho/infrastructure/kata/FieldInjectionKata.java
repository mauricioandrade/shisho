package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;

/** {@code @Autowired} (or {@code @Inject}) on a field. */
final class FieldInjectionKata implements Kata {

    @Override
    public String id() {
        return "field-injection";
    }

    @Override
    public Severity severity() {
        return Severity.GRAVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.BASELINE;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        return unit.fields().stream()
                .flatMap(field -> Annotations.find(field.annotations(), Annotations.INJECTION)
                        .map(annotation -> lesson(unit, annotation.line(), field.name()))
                        .stream())
                .toList();
    }
}
