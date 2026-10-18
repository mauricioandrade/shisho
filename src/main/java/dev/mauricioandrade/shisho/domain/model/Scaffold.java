package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * How the catalog generates a CRUD in one architecture: the files, as templates for their path
 * and content, and what each one is for. The schema is documented in {@code catalog/README.md}.
 *
 * @param architectureId       the catalog architecture this scaffold follows
 * @param buildGroup           the groupId that, declared in the build, makes this the scaffold of
 *                             the project's stack; empty for the default stack
 * @param requiredDependencies what the generated code needs in the build: for each dependency,
 *                             the artifactIds that provide it (Spring Boot 4 renamed some starters)
 * @param refusedImports       packages that, imported anywhere in the project, mean the generated
 *                             code would not compile alongside it, such as an older namespace
 * @param minimumPlatforms     the lowest major version of each platform (by groupId) the generated
 *                             code compiles on, when the build pins one
 */
public record Scaffold(String architectureId, Optional<String> buildGroup, List<List<String>> requiredDependencies,
                       List<String> refusedImports, Map<String, Integer> minimumPlatforms, List<File> files) {

    public Scaffold {
        requiredDependencies = requiredDependencies.stream().map(List::copyOf).toList();
        refusedImports = List.copyOf(refusedImports);
        minimumPlatforms = Map.copyOf(minimumPlatforms);
        files = List.copyOf(files);
    }

    /**
     * @param path        template of the path, relative to the project's base package
     * @param explanation template of why the file exists, shown when it is created
     * @param content     template of the file
     * @param onceUnless  for a file shared by every entity: the annotation that, on any class of
     *                    the project, means the file is not needed
     */
    public record File(String path, String explanation, String content, Optional<String> onceUnless) {
    }
}
