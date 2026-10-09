package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Method;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A request mapping that returns or receives (as {@code @RequestBody}) a JPA {@code @Entity},
 * also inside generics such as {@code ResponseEntity<List<User>>}. Entities are matched by
 * simple name, so the kata needs the whole project.
 */
final class EntityInControllerKata implements ProjectKata {

    private static final Set<String> ENTITY = Set.of("Entity");
    private static final Set<String> CONTROLLER = Set.of("Controller", "RestController");
    private static final Set<String> MAPPING = Set.of(
            "RequestMapping", "GetMapping", "PostMapping", "PutMapping", "PatchMapping", "DeleteMapping");
    private static final Set<String> REQUEST_BODY = Set.of("RequestBody");
    private static final Pattern TYPE_NAME = Pattern.compile("[\\w$.]+");

    @Override
    public String id() {
        return "entity-in-controller";
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
    public List<Lesson> evaluate(ProjectModel project) {
        Set<String> entities = project.classes().stream()
                .filter(unit -> Annotations.has(unit.annotations(), ENTITY))
                .map(ClassUnit::name)
                .collect(Collectors.toSet());
        if (entities.isEmpty()) {
            return List.of();
        }
        return project.classes().stream()
                .filter(unit -> Annotations.has(unit.annotations(), CONTROLLER))
                .flatMap(controller -> controller.methods().stream()
                        .filter(method -> Annotations.has(method.annotations(), MAPPING))
                        .flatMap(method -> exposedTypes(method)
                                .filter(entities::contains)
                                .distinct()
                                .map(entity -> lesson(controller.file(), method.line(), entity, method.name()))))
                .toList();
    }

    private static Stream<String> exposedTypes(Method method) {
        Stream<String> bodies = method.parameters().stream()
                .filter(parameter -> Annotations.has(parameter.annotations(), REQUEST_BODY))
                .map(ClassUnit.Parameter::type);
        return Stream.concat(Stream.of(method.returnType()), bodies).flatMap(EntityInControllerKata::simpleNames);
    }

    private static Stream<String> simpleNames(String type) {
        return TYPE_NAME.matcher(type).results()
                .map(match -> match.group().substring(match.group().lastIndexOf('.') + 1));
    }
}
