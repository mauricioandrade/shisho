package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.Detector;

import java.util.List;

public final class Detectors {

    private Detectors() {
    }

    /** Every detector the Shishō implements; catalog signals without one here are not measured yet. */
    public static List<Detector> all() {
        return List.of(
                new OneUseCasePerCrudMethodDetector(),
                new AnemicDomainDetector(),
                new DomainEntityIdenticalToJpaEntityDetector(),
                new MapperBetweenIdenticalStructuresDetector(),
                new CrowdedServicePackageDetector(),
                new GodServiceDetector(),
                new CyclicServiceDependencyDetector());
    }
}
