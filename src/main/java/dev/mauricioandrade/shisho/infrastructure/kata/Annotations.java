package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Annotation lookups by simple name, so {@code @Autowired} and its qualified form both match. */
final class Annotations {

    static final Set<String> INJECTION = Set.of("Autowired", "Inject");
    /** Spring stereotypes, CDI scopes (Quarkus) and Jakarta REST resources, which CDI makes beans. */
    static final Set<String> BEANS = Set.of(
            "Component", "Service", "Repository", "Controller", "RestController",
            "Configuration", "ControllerAdvice", "RestControllerAdvice",
            "ApplicationScoped", "Singleton", "RequestScoped", "Dependent", "SessionScoped", "Path");

    private Annotations() {
    }

    static Optional<Annotation> find(List<Annotation> annotations, Set<String> simpleNames) {
        return annotations.stream().filter(annotation -> simpleNames.contains(annotation.simpleName())).findFirst();
    }

    static boolean has(List<Annotation> annotations, Set<String> simpleNames) {
        return find(annotations, simpleNames).isPresent();
    }
}
