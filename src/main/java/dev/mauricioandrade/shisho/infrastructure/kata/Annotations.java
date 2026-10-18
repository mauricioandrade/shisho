package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;

import java.util.List;
import java.util.HashSet;
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

    /**
     * {@link #BEANS} plus the project's own stereotypes: annotation types annotated with one of them,
     * such as a {@code @UseCase} annotated with {@code @Component}, which make a bean of what they
     * annotate. Stereotypes of stereotypes count too.
     */
    static Set<String> beanAnnotations(List<ClassUnit> classes) {
        Set<String> beans = new HashSet<>(BEANS);
        boolean grew = true;
        while (grew) {
            grew = false;
            for (ClassUnit unit : classes) {
                if (unit.kind() == ClassUnit.Kind.ANNOTATION && has(unit.annotations(), beans) && beans.add(unit.name())) {
                    grew = true;
                }
            }
        }
        return Set.copyOf(beans);
    }

    static Optional<Annotation> find(List<Annotation> annotations, Set<String> simpleNames) {
        return annotations.stream().filter(annotation -> simpleNames.contains(annotation.simpleName())).findFirst();
    }

    static boolean has(List<Annotation> annotations, Set<String> simpleNames) {
        return find(annotations, simpleNames).isPresent();
    }
}
