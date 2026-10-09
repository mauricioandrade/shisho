package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * What {@code shisho crud} is about to create; nothing is written yet.
 *
 * @param skipped             files shared by every entity that the project already covers
 * @param otherArchitecture   the architecture the project follows, when it is not the one asked for
 * @param missingDependencies dependencies the generated code needs and the build lacks, each as
 *                            the artifactIds that would provide it
 * @param cost                present when the architecture costs more than a simple one and is
 *                            being chosen now, not followed: see docs/adr/0006-crud-cost-warning.md
 */
public record CrudPlan(ArchitectureProfile architecture, List<GeneratedFile> files, List<GeneratedFile> skipped,
                       Optional<ArchitectureProfile> otherArchitecture, List<List<String>> missingDependencies,
                       Optional<Cost> cost) {

    public CrudPlan {
        files = List.copyOf(files);
        skipped = List.copyOf(skipped);
        missingDependencies = missingDependencies.stream().map(List::copyOf).toList();
    }

    /** {@code paidBy}: the declared context keys that pay for the architecture's cost; none means nothing does. */
    public record Cost(List<String> paidBy) {

        public Cost {
            paidBy = List.copyOf(paidBy);
        }

        public boolean paid() {
            return !paidBy.isEmpty();
        }
    }
}
