package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** A {@code service} package with more than {@value #MAXIMUM_CLASSES} classes: the layer stopped scaling. */
final class CrowdedServicePackageDetector implements Detector {

    static final int MAXIMUM_CLASSES = 15;
    private static final Set<String> SERVICE_PACKAGES = Set.of("service", "services");

    @Override
    public String id() {
        return "pacote_service_com_mais_de_15_classes";
    }

    @Override
    public Optional<Evidence> detect(ProjectModel project) {
        Map<String, Long> classesByPackage = project.classes().stream()
                .filter(unit -> SERVICE_PACKAGES.contains(lastSegment(unit.packageName())))
                .collect(Collectors.groupingBy(ClassUnit::packageName, Collectors.counting()));
        return classesByPackage.entrySet().stream()
                .filter(entry -> entry.getValue() > MAXIMUM_CLASSES)
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .flatMap(entry -> evidence(entry.getKey(), entry.getValue()));
    }

    private static String lastSegment(String packageName) {
        return packageName.substring(packageName.lastIndexOf('.') + 1);
    }
}
