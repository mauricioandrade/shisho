package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Kind;
import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.PackagePattern;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Most classes with state in a {@code domain} package have nothing but accessors: the rules live
 * elsewhere and the domain layer only carries data. Records are left out, since a value object
 * with no behavior is legitimate.
 */
final class AnemicDomainDetector implements Detector {

    static final int MINIMUM_CLASSES = 2;
    private static final PackagePattern DOMAIN = PackagePattern.parse("domain");
    private static final Pattern ACCESSOR = Pattern.compile("(get|set|is)\\p{Lu}\\w*");
    private static final Set<String> OBJECT_METHODS = Set.of("equals", "hashCode", "toString");

    @Override
    public String id() {
        return "dominio_anemico";
    }

    @Override
    public Optional<Evidence> detect(ProjectModel project) {
        List<ClassUnit> domainClasses = project.classes().stream()
                .filter(unit -> unit.kind() == Kind.CLASS && DOMAIN.matches(unit.packageName()))
                .filter(unit -> !unit.stateNames().isEmpty())
                .toList();
        long anemic = domainClasses.stream().filter(AnemicDomainDetector::onlyAccessors).count();
        if (anemic < MINIMUM_CLASSES || anemic * 2 <= domainClasses.size()) {
            return Optional.empty();
        }
        return evidence(anemic, domainClasses.size());
    }

    private static boolean onlyAccessors(ClassUnit unit) {
        return unit.methods().stream().allMatch(method ->
                ACCESSOR.matcher(method.name()).matches() || OBJECT_METHODS.contains(method.name()));
    }
}
