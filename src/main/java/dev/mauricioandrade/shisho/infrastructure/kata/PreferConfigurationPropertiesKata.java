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
 * prefix ({@code ${mail.host}}, {@code ${mail.port}}, ...), wherever they are declared; or as many
 * MicroProfile {@code @ConfigProperty(name = "mail.host")}, the Quarkus way.
 */
final class PreferConfigurationPropertiesKata implements ClassKata {

    static final int MINIMUM_PROPERTIES = 3;
    private static final Set<String> VALUE = Set.of("Value");
    private static final Set<String> CONFIG_PROPERTY = Set.of("ConfigProperty");
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
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ClassUnit unit) {
        Map<Group, List<Annotation>> byPrefix = annotations(unit)
                .filter(annotation -> group(annotation).isPresent())
                .collect(Collectors.groupingBy(annotation -> group(annotation).orElseThrow()));
        return byPrefix.entrySet().stream()
                .filter(group -> group.getValue().size() >= MINIMUM_PROPERTIES)
                .map(group -> lesson(unit, group.getKey(), firstLine(group.getValue()), group.getValue().size()))
                .sorted(Comparator.comparingInt(Lesson::line))
                .toList();
    }

    /** Properties injected with the same annotation and sharing a prefix. */
    private record Group(String prefix, boolean configProperty) {
    }

    private Lesson lesson(ClassUnit unit, Group group, int line, int size) {
        return group.configProperty()
                ? QuarkusApplications.lesson(this, unit.file(), line, group.prefix(), size)
                : lesson(unit, line, group.prefix(), size);
    }

    private static Optional<Group> group(Annotation annotation) {
        if (Annotations.has(List.of(annotation), CONFIG_PROPERTY)) {
            return annotation.attribute("name").flatMap(PreferConfigurationPropertiesKata::prefix)
                    .map(prefix -> new Group(prefix, true));
        }
        return annotation.attribute("value")
                .map(PLACEHOLDER::matcher)
                .filter(Matcher::find)
                .flatMap(matcher -> prefix(matcher.group(1)))
                .map(prefix -> new Group(prefix, false));
    }

    private static Stream<Annotation> annotations(ClassUnit unit) {
        Stream<Parameter> parameters = Stream.concat(
                unit.constructors().stream().flatMap(constructor -> constructor.parameters().stream()),
                unit.methods().stream().flatMap(method -> method.parameters().stream()));
        return Stream.of(
                        unit.fields().stream().map(Field::annotations),
                        unit.methods().stream().map(Method::annotations),
                        parameters.map(Parameter::annotations))
                .flatMap(annotations -> annotations)
                .flatMap(annotations -> Stream.concat(
                        Annotations.find(annotations, VALUE).stream(), Annotations.find(annotations, CONFIG_PROPERTY).stream()));
    }

    /** {@code mail} for {@code mail.host} (from {@code ${mail.host:localhost}}); empty for a key with no dot. */
    private static Optional<String> prefix(String key) {
        String stripped = key.strip();
        return stripped.contains(".")
                ? Optional.of(stripped.substring(0, stripped.lastIndexOf('.')))
                : Optional.empty();
    }

    private static int firstLine(List<Annotation> values) {
        return values.stream().mapToInt(Annotation::line).min().orElseThrow();
    }
}
