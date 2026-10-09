package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Optional;

/**
 * How the catalog generates a CRUD in one architecture: the files, as templates for their path
 * and content, and what each one is for. The schema is documented in {@code catalog/README.md}.
 *
 * @param architectureId       the catalog architecture this scaffold follows
 * @param requiredDependencies artifactIds the generated code needs in the build
 */
public record Scaffold(String architectureId, List<String> requiredDependencies, List<File> files) {

    public Scaffold {
        requiredDependencies = List.copyOf(requiredDependencies);
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
