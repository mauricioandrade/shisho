package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Parameter;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A request body without {@code @Valid}/{@code @Validated}, for bodies that have something to validate:
 * Spring's {@code @RequestBody}, or the body of a Jakarta REST resource method (Quarkus). A body
 * whose type is a class of the project is flagged only if that class declares a constraint: without
 * one, {@code @Valid} would check nothing. A type from outside the project is flagged, since its
 * annotations cannot be seen. The lesson's second argument is the {@code @Valid} to import,
 * {@code javax} or {@code jakarta}, for {@code shisho fix}.
 */
final class RequestBodyWithoutValidKata implements ProjectKata {

    private static final Set<String> REQUEST_BODY = Set.of("RequestBody");
    private static final Set<String> VALIDATION = Set.of("Valid", "Validated");
    private static final Set<String> CONSTRAINT = Set.of("Constraint");
    private static final List<String> VALIDATION_PACKAGES = List.of("jakarta.validation", "javax.validation", "org.hibernate.validator");
    private static final Pattern TYPE_NAME = Pattern.compile("[\\w$.]+");

    /**
     * Types with nothing for {@code @Valid} to check: JDK values, dates and collections, and the
     * framework's raw bodies. A body made only of them ({@code Set<Long>}, {@code Long[]},
     * {@code Map<String, Object>}) has no constraint anywhere.
     */
    private static final Set<String> UNSTRUCTURED = Set.of(
            "String", "Object", "Integer", "Long", "Short", "Byte", "Double", "Float", "Boolean", "Character",
            "int", "long", "short", "byte", "double", "float", "boolean", "char",
            "BigDecimal", "BigInteger", "UUID", "LocalDate", "LocalDateTime", "LocalTime", "Instant",
            "OffsetDateTime", "ZonedDateTime", "Duration",
            "List", "Set", "Collection", "Iterable", "Map", "Optional",
            "MultipartFile", "JsonNode");

    @Override
    public String id() {
        return "request-body-without-valid";
    }

    @Override
    public Severity severity() {
        return Severity.MEDIA;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        Constraints constraints = new Constraints(project.classes());
        String valid = JavaxProjects.valid(project);
        return project.classes().stream()
                .flatMap(unit -> evaluate(unit, constraints, valid))
                .toList();
    }

    private Stream<Lesson> evaluate(ClassUnit unit, Constraints constraints, String valid) {
        Stream<Lesson> spring = unit.methods().stream()
                .flatMap(method -> method.parameters().stream())
                .filter(parameter -> lacksValidation(parameter, constraints))
                .flatMap(parameter -> Annotations.find(parameter.annotations(), REQUEST_BODY)
                        .map(annotation -> lesson(unit.file(), annotation.line(), parameter.type(), valid))
                        .stream());
        Stream<Lesson> jakartaRest = unit.methods().stream()
                .filter(JakartaRest::isResourceMethod)
                .flatMap(method -> JakartaRest.body(unit, method).stream())
                .filter(parameter -> lacksValidation(parameter, constraints))
                .map(parameter -> QuarkusApplications.lesson(this, unit.file(), parameter.line(), parameter.type(), valid));
        return Stream.concat(spring, jakartaRest);
    }

    private static boolean lacksValidation(Parameter parameter, Constraints constraints) {
        return !Annotations.has(parameter.annotations(), VALIDATION) && hasStructuredBody(parameter)
                && constraints.mayHave(parameter.type());
    }

    private static boolean hasStructuredBody(Parameter parameter) {
        return !simpleNames(parameter.type()).allMatch(UNSTRUCTURED::contains);
    }

    /** The simple names in a type: {@code List} and {@code UserDto} in {@code java.util.List<UserDto>}. */
    private static Stream<String> simpleNames(String type) {
        return TYPE_NAME.matcher(type).results().map(match -> match.group().substring(match.group().lastIndexOf('.') + 1));
    }

    /** Which classes of the project, by simple name, declare a Bean Validation constraint. */
    private static final class Constraints {

        private final Map<String, List<ClassUnit>> classes;
        private final Set<String> customConstraints;

        Constraints(List<ClassUnit> units) {
            classes = units.stream().collect(Collectors.groupingBy(ClassUnit::name));
            customConstraints = units.stream()
                    .filter(unit -> unit.kind() == ClassUnit.Kind.ANNOTATION && Annotations.has(unit.annotations(), CONSTRAINT))
                    .map(ClassUnit::name)
                    .collect(Collectors.toSet());
        }

        /**
         * False only when every name in {@code type} that is a class of the project ({@code UserDto}
         * in {@code List<UserDto>}) declares no constraint; true when none is.
         */
        boolean mayHave(String type) {
            List<String> known = simpleNames(type).filter(classes::containsKey).toList();
            return known.isEmpty() || known.stream().anyMatch(name -> declares(name, new HashSet<>()));
        }

        /** A constraint in the class, in a class of the same name or in one it extends. */
        private boolean declares(String name, Set<String> visited) {
            if (!visited.add(name)) {
                return false;
            }
            return classes.getOrDefault(name, List.of()).stream().anyMatch(unit -> declaresItself(unit)
                    || unit.supertypes().stream().anyMatch(supertype -> declares(supertype, visited)));
        }

        /**
         * Read through the file's imports: a file importing from Bean Validation uses it. A
         * constraint written fully qualified or declared in the project counts as well.
         */
        private boolean declaresItself(ClassUnit unit) {
            return unit.imports().stream().anyMatch(declaration -> isValidation(declaration.name()))
                    || annotations(unit).anyMatch(annotation -> isValidation(annotation.name())
                            || customConstraints.contains(annotation.simpleName()));
        }

        private static Stream<Annotation> annotations(ClassUnit unit) {
            return Stream.of(
                    unit.annotations().stream(),
                    unit.fields().stream().flatMap(field -> field.annotations().stream()),
                    unit.constructors().stream().flatMap(constructor -> constructor.parameters().stream())
                            .flatMap(parameter -> parameter.annotations().stream()),
                    unit.methods().stream().flatMap(method -> method.annotations().stream()))
                    .flatMap(stream -> stream);
        }

        private static boolean isValidation(String name) {
            return VALIDATION_PACKAGES.stream().anyMatch(name::startsWith);
        }
    }
}
