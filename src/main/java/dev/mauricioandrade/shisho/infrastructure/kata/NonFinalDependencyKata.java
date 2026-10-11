package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Field;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Parameter;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A bean's field (Spring or CDI) that receives a dependency through the constructor but is not
 * {@code final}. A field counts as constructor-injected when its type is a constructor parameter
 * type, or when Lombok's {@code @AllArgsConstructor} puts every field in the constructor.
 * Fields with {@code @Autowired} or {@code @Value} are left to their own katas.
 */
final class NonFinalDependencyKata implements ClassKata {

    private static final Set<String> HANDLED_ELSEWHERE = Set.of("Autowired", "Inject", "Value");

    @Override
    public String id() {
        return "non-final-dependency";
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
        if (!Annotations.has(unit.annotations(), Annotations.BEANS)) {
            return List.of();
        }
        boolean allArgsConstructor = Annotations.has(unit.annotations(), Set.of("AllArgsConstructor"));
        Set<String> constructorTypes = unit.constructors().stream()
                .flatMap(constructor -> constructor.parameters().stream())
                .map(Parameter::type)
                .collect(Collectors.toSet());
        return unit.fields().stream()
                .filter(NonFinalDependencyKata::isMutableInstanceField)
                .filter(field -> !Annotations.has(field.annotations(), HANDLED_ELSEWHERE))
                .filter(field -> allArgsConstructor || constructorTypes.contains(field.type()))
                .map(field -> lesson(unit, field.line(), field.name()))
                .toList();
    }

    private static boolean isMutableInstanceField(Field field) {
        return !field.isFinal() && !field.isStatic() && !field.hasInitializer();
    }
}
