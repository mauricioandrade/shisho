package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.application.usecase.ProjectLoader.LoadedProject;
import dev.mauricioandrade.shisho.domain.exception.ScaffoldException;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.CrudPlan;
import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.GeneratedFile;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Scaffold;
import dev.mauricioandrade.shisho.domain.port.in.ClassifyArchitecture;
import dev.mauricioandrade.shisho.domain.port.in.GenerateCrud;
import dev.mauricioandrade.shisho.domain.port.out.BuildFileReader;
import dev.mauricioandrade.shisho.domain.port.out.CatalogRepository;
import dev.mauricioandrade.shisho.domain.port.out.ProjectConfigReader;
import dev.mauricioandrade.shisho.domain.port.out.ScaffoldRenderer;
import dev.mauricioandrade.shisho.domain.port.out.SourceParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Picks the catalog template for the requested or the detected architecture, places it under
 * the project's base package and leaves out the shared files the project already has. See
 * docs/adr/0005-crud-scaffolding.md.
 */
public final class GenerateCrudService implements GenerateCrud {

    private final ProjectLoader projectLoader;
    private final ClassifyArchitecture classifyArchitecture;
    private final CatalogRepository catalog;
    private final ScaffoldRenderer renderer;

    public GenerateCrudService(
            BuildFileReader buildFileReader,
            ProjectConfigReader configReader,
            SourceParser sourceParser,
            ClassifyArchitecture classifyArchitecture,
            CatalogRepository catalog,
            ScaffoldRenderer renderer) {
        this.projectLoader = new ProjectLoader(buildFileReader, configReader, sourceParser);
        this.classifyArchitecture = classifyArchitecture;
        this.catalog = catalog;
        this.renderer = renderer;
    }

    @Override
    public CrudPlan plan(Path projectRoot, CrudRequest request) {
        LoadedProject loaded = projectLoader.load(projectRoot, config -> { });
        ProjectModel project = loaded.model();
        Optional<ArchitectureProfile> detected = classifyArchitecture.classify(project, loaded.config().context())
                .map(ArchitectureMatch::architecture);

        Scaffold scaffold = scaffold(request, detected);
        ArchitectureProfile architecture = catalog.architectures().stream()
                .filter(profile -> profile.id().equals(scaffold.architectureId()))
                .findFirst()
                .orElseThrow();
        String basePackage = request.basePackage().or(() -> basePackage(project))
                .orElseThrow(() -> new ScaffoldException("crud.error.no-package"));

        List<GeneratedFile> rendered = renderer.render(scaffold, basePackage, request);
        List<GeneratedFile> files = new ArrayList<>();
        List<GeneratedFile> skipped = new ArrayList<>();
        for (int i = 0; i < rendered.size(); i++) {
            boolean covered = scaffold.files().get(i).onceUnless().filter(annotation -> annotated(project, annotation)).isPresent();
            (covered ? skipped : files).add(rendered.get(i));
        }
        return new CrudPlan(architecture, files, skipped,
                detected.filter(other -> !other.id().equals(architecture.id())),
                missingDependencies(scaffold, project));
    }

    private Scaffold scaffold(CrudRequest request, Optional<ArchitectureProfile> detected) {
        List<Scaffold> scaffolds = catalog.scaffolds();
        String available = scaffolds.stream().map(Scaffold::architectureId).collect(Collectors.joining(", "));
        if (request.architectureId().isEmpty() && detected.isEmpty()) {
            throw new ScaffoldException("crud.error.no-architecture", available);
        }
        String id = request.architectureId().orElseGet(() -> detected.orElseThrow().id());
        return scaffolds.stream().filter(scaffold -> scaffold.architectureId().equals(id)).findFirst().orElseThrow(() ->
                request.architectureId().isPresent()
                        ? new ScaffoldException("crud.error.unknown-architecture", id, available)
                        : new ScaffoldException("crud.error.no-scaffold", detected.orElseThrow().name(), available));
    }

    /** The longest package every class shares: in a Spring Initializr project, the application's package. */
    private static Optional<String> basePackage(ProjectModel project) {
        List<List<String>> packages = project.classes().stream()
                .map(ClassUnit::packageName)
                .filter(name -> !name.isEmpty())
                .distinct()
                .map(name -> Arrays.asList(name.split("\\.")))
                .toList();
        if (packages.isEmpty()) {
            return Optional.empty();
        }
        List<String> common = packages.getFirst();
        for (List<String> segments : packages) {
            int shared = 0;
            while (shared < common.size() && shared < segments.size() && common.get(shared).equals(segments.get(shared))) {
                shared++;
            }
            common = common.subList(0, shared);
        }
        return common.isEmpty() ? Optional.empty() : Optional.of(String.join(".", common));
    }

    private static boolean annotated(ProjectModel project, String annotation) {
        String simpleName = annotation.startsWith("@") ? annotation.substring(1) : annotation;
        return project.classes().stream().anyMatch(unit -> unit.isAnnotatedWith(simpleName));
    }

    private static List<String> missingDependencies(Scaffold scaffold, ProjectModel project) {
        Set<String> present = project.build().stream()
                .flatMap(build -> build.dependencies().stream())
                .map(Dependency::artifactId)
                .collect(Collectors.toSet());
        return scaffold.requiredDependencies().stream().filter(artifactId -> !present.contains(artifactId)).toList();
    }
}
