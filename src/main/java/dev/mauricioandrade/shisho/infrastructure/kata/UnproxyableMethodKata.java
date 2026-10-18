package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Method;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * An annotation that only works through a Spring proxy on a method the proxy cannot reach, so it is
 * silently ignored: {@code private}, {@code static} or {@code final}, and anything but
 * {@code public} on Spring Boot 2, whose proxies intercept public methods only. Quarkus is left
 * out: since 3.0 an interceptor binding on a private method fails the build, and ArC intercepts
 * static methods. A call from inside the class (self-invocation) skips the proxy too, but the model
 * does not say which method makes each call.
 */
final class UnproxyableMethodKata implements ProjectKata {

    private static final Set<String> PROXIED = Set.of(
            "Transactional", "Async", "Cacheable", "CachePut", "CacheEvict", "Caching", "Retryable");
    private static final List<String> VISIBILITIES = List.of("public", "protected", "private");

    @Override
    public String id() {
        return "unproxyable-method";
    }

    @Override
    public Severity severity() {
        return Severity.GRAVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        if (project.build().filter(QuarkusApplications::isQuarkusApplication).isPresent()) {
            return List.of();
        }
        boolean springBoot2 = JavaxProjects.isJavax(project);
        return project.classes().stream()
                .filter(unit -> unit.kind() == ClassUnit.Kind.CLASS)
                .flatMap(unit -> unit.methods().stream().flatMap(method -> Annotations.find(method.annotations(), PROXIED)
                        .flatMap(annotation -> lesson(unit, method, annotation.simpleName(), springBoot2))
                        .stream()))
                .toList();
    }

    private Optional<Lesson> lesson(ClassUnit unit, Method method, String annotation, boolean springBoot2) {
        Optional<String> unreachable = Stream.of("private", "static", "final")
                .filter(method.modifiers()::contains)
                .findFirst();
        if (unreachable.isPresent()) {
            return Optional.of(lesson(unit.file(), method.line(), annotation, method.name(), unreachable.get()));
        }
        if (springBoot2 && !method.modifiers().contains("public")) {
            String visibility = VISIBILITIES.stream().filter(method.modifiers()::contains).findFirst().orElse("package-private");
            return Optional.of(JavaxProjects.springBoot2Lesson(this, unit.file(), method.line(), annotation, method.name(), visibility));
        }
        return Optional.empty();
    }
}
