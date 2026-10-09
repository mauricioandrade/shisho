package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassKata;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Field;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Method;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Parameter;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * At least {@value #MINIMUM_PROPERTIES} {@code @Value} placeholders in one class sharing the same
 * prefix ({@code ${mail.host}}, {@code ${mail.port}}, ...), wherever they are declared.
 */
final class PreferConfigurationPropertiesKata implements ClassKata {

    static final int MINIMUM_PROPERTIES = 3;
    private static final Set<String> VALUE = Set.of("Value");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}:]+)");

    @Override
    public String id() {
        return "prefer-configuration-properties";
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
        Map<String, List<Annotation>> byPrefix = values(unit)
                .filter(value -> prefix(value).isPresent())
                .collect(Collectors.groupingBy(value -> prefix(value).orElseThrow()));
        return byPrefix.entrySet().stream()
                .filter(group -> group.getValue().size() >= MINIMUM_PROPERTIES)
                .map(group -> lesson(unit, firstLine(group.getValue()), group.getKey(), group.getValue().size()))
                .sorted(Comparator.comparingInt(Lesson::line))
                .toList();
    }

    private static Stream<Annotation> values(ClassUnit unit) {
        Stream<Parameter> parameters = Stream.concat(
                unit.constructors().stream().flatMap(constructor -> constructor.parameters().stream()),
                unit.methods().stream().flatMap(method -> method.parameters().stream()));
        return Stream.of(
                        unit.fields().stream().map(Field::annotations),
                        unit.methods().stream().map(Method::annotations),
                        parameters.map(Parameter::annotations))
                .flatMap(annotations -> annotations)
                .flatMap(annotations -> Annotations.find(annotations, VALUE).stream());
    }

    /** {@code mail} for {@code ${mail.host:localhost}}; empty for a key with no dot. */
    private static Optional<String> prefix(Annotation value) {
        return value.attribute("value")
                .map(PLACEHOLDER::matcher)
                .filter(Matcher::find)
                .map(matcher -> matcher.group(1).strip())
                .filter(key -> key.contains("."))
                .map(key -> key.substring(0, key.lastIndexOf('.')));
    }

    private static int firstLine(List<Annotation> values) {
        return values.stream().mapToInt(Annotation::line).min().orElseThrow();
    }
}
