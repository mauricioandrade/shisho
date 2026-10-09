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
 */
public record CrudPlan(ArchitectureProfile architecture, List<GeneratedFile> files, List<GeneratedFile> skipped,
                       Optional<ArchitectureProfile> otherArchitecture, List<List<String>> missingDependencies) {

    public CrudPlan {
        files = List.copyOf(files);
        skipped = List.copyOf(skipped);
        missingDependencies = missingDependencies.stream().map(List::copyOf).toList();
    }
}
