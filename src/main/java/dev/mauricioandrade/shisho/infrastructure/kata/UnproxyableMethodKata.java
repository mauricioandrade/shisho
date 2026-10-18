package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Invocation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Method;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An annotation that only works through a Spring proxy on a method the proxy cannot reach, so it is
 * silently ignored: {@code private}, {@code static} or {@code final}, and anything but
 * {@code public} on Spring Boot 2, whose proxies intercept public methods only. Quarkus is left
 * out: since 3.0 an interceptor binding on a private method fails the build, and ArC intercepts
 * static methods, self-invocation included.
 *
 * <p>A call from inside the class ({@code save()} or {@code this.save()}) skips the proxy too. It is
 * flagged when the annotation is lost on that call: always for {@code @Async} and the caching
 * ones; for {@code @Transactional}, when the calling method is not already in a transaction (nor
 * its class), or when the called one asks for its own {@code propagation}. A call from a
 * constructor runs before there is any proxy. When the name and the number of arguments point at
 * more than one method and one of them lacks the annotation, the call is left alone.
 */
final class UnproxyableMethodKata implements ProjectKata {

    private static final Set<String> PROXIED = Set.of(
            "Transactional", "Async", "Cacheable", "CachePut", "CacheEvict", "Caching", "Retryable");
    private static final Set<String> TRANSACTIONAL = Set.of("Transactional");
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
                .flatMap(unit -> Stream.concat(
                        unit.methods().stream().flatMap(method -> Annotations.find(method.annotations(), PROXIED)
                                .flatMap(annotation -> lesson(unit, method, annotation.simpleName(), springBoot2))
                                .stream()),
                        selfInvocations(unit, springBoot2)))
                .toList();
    }

    private Stream<Lesson> selfInvocations(ClassUnit unit, boolean springBoot2) {
        Map<String, List<Method>> methods = unit.methods().stream().collect(Collectors.groupingBy(Method::name));
        return unit.invocations().stream()
                .filter(call -> call.target().filter(target -> !target.equals("this")).isEmpty())
                .flatMap(call -> callee(methods, call)
                        .filter(callee -> reachable(callee, springBoot2))
                        .flatMap(callee -> Annotations.find(callee.annotations(), PROXIED)
                                .filter(annotation -> ignoredOn(unit, methods, call, annotation)))
                        .map(annotation -> new Lesson(id(), severity(), unit.file(), call.line(),
                                "kata." + id() + ".lesson.self-invocation", List.of(annotation.simpleName(), call.name())))
                        .stream());
    }

    /** The one method the call can reach, when every candidate carries a proxied annotation. */
    private static Optional<Method> callee(Map<String, List<Method>> methods, Invocation call) {
        List<Method> candidates = methods.getOrDefault(call.name(), List.of()).stream()
                .filter(method -> call.arguments() == Invocation.UNKNOWN_ARGUMENTS || method.parameters().size() == call.arguments())
                .toList();
        boolean allProxied = !candidates.isEmpty()
                && candidates.stream().allMatch(method -> Annotations.has(method.annotations(), PROXIED));
        return allProxied ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    /**
     * Whether skipping the proxy loses the annotation on this call. A transaction the caller
     * already runs in covers the callee, unless the callee asks for a {@code propagation} of its own.
     */
    private static boolean ignoredOn(ClassUnit unit, Map<String, List<Method>> methods, Invocation call, Annotation annotation) {
        if (!TRANSACTIONAL.contains(annotation.simpleName())) {
            return true;
        }
        boolean callerInTransaction = Annotations.has(unit.annotations(), TRANSACTIONAL)
                || call.caller().stream().flatMap(caller -> methods.getOrDefault(caller, List.of()).stream())
                        .anyMatch(caller -> Annotations.has(caller.annotations(), TRANSACTIONAL));
        boolean fromConstructor = call.caller().isEmpty();
        return fromConstructor || !callerInTransaction || annotation.attribute("propagation").isPresent();
    }

    private Optional<Lesson> lesson(ClassUnit unit, Method method, String annotation, boolean springBoot2) {
        Optional<String> unreachable = unreachableBy(method);
        if (unreachable.isPresent()) {
            return Optional.of(lesson(unit.file(), method.line(), annotation, method.name(), unreachable.get()));
        }
        if (hiddenOnSpringBoot2(method, springBoot2)) {
            String visibility = VISIBILITIES.stream().filter(method.modifiers()::contains).findFirst().orElse("package-private");
            return Optional.of(JavaxProjects.springBoot2Lesson(this, unit.file(), method.line(), annotation, method.name(), visibility));
        }
        return Optional.empty();
    }

    /** Whether the proxy can intercept the method at all; one it cannot already has its own lesson. */
    private static boolean reachable(Method method, boolean springBoot2) {
        return unreachableBy(method).isEmpty() && !hiddenOnSpringBoot2(method, springBoot2);
    }

    /** The modifier no proxy gets past. */
    private static Optional<String> unreachableBy(Method method) {
        return Stream.of("private", "static", "final").filter(method.modifiers()::contains).findFirst();
    }

    private static boolean hiddenOnSpringBoot2(Method method, boolean springBoot2) {
        return springBoot2 && !method.modifiers().contains("public");
    }
}
