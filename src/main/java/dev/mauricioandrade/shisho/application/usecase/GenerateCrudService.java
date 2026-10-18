package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.application.usecase.ProjectLoader.LoadedProject;
import dev.mauricioandrade.shisho.domain.exception.ScaffoldException;
import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.CrudPlan;
import dev.mauricioandrade.shisho.domain.model.CrudRequest;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.GeneratedFile;
import dev.mauricioandrade.shisho.domain.model.ProjectContext;
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
        if (!project.javaVersion().isAtLeast(MINIMUM_JAVA)) {
            throw new ScaffoldException("crud.error.java-version", project.javaVersion(), MINIMUM_JAVA);
        }
        Optional<ProjectContext> context = loaded.config().context();
        Optional<String> knownBasePackage = request.basePackage().or(() -> basePackage(project));
        Optional<ArchitectureProfile> detected = classifyArchitecture.classify(project, context)
                .map(ArchitectureMatch::architecture)
                .or(() -> knownBasePackage.flatMap(base -> generatedBefore(project, base)));

        Scaffold scaffold = scaffold(request, detected, scaffolds(project));
        ArchitectureProfile architecture = architecture(scaffold.architectureId());
        String basePackage = knownBasePackage.orElseThrow(() -> new ScaffoldException("crud.error.no-package"));

        List<GeneratedFile> rendered = renderer.render(scaffold, basePackage, request);
        List<GeneratedFile> files = new ArrayList<>();
        List<GeneratedFile> skipped = new ArrayList<>();
        for (int i = 0; i < rendered.size(); i++) {
            boolean covered = scaffold.files().get(i).onceUnless().filter(annotation -> annotated(project, annotation)).isPresent();
            (covered ? skipped : files).add(rendered.get(i));
        }
        return new CrudPlan(architecture, files, skipped,
                detected.filter(other -> !other.id().equals(architecture.id())),
                missingDependencies(scaffold, project), cost(architecture, detected, context));
    }

    /**
     * The architecture of a CRUD this command generated before, recognized by its template's
     * paths: every file of one entity is where the template puts it. The classifier needs two
     * features to tell package-by-feature from any package, and the second crud only has one.
     */
    private Optional<ArchitectureProfile> generatedBefore(ProjectModel project, String basePackage) {
        Set<Path> files = project.classes().stream().map(ClassUnit::file).collect(Collectors.toSet());
        List<String> names = project.classes().stream().map(ClassUnit::name).distinct().sorted().toList();
        for (Scaffold scaffold : scaffolds(project)) {
            for (String name : names) {
                if (generated(scaffold, basePackage, name, files)) {
                    return Optional.of(architecture(scaffold.architectureId()));
                }
            }
        }
        return Optional.empty();
    }

    private boolean generated(Scaffold scaffold, String basePackage, String entity, Set<Path> files) {
        List<Path> paths = renderer.paths(scaffold, basePackage, entity);
        if (paths.isEmpty()) {
            return false;
        }
        for (int i = 0; i < paths.size(); i++) {
            if (scaffold.files().get(i).onceUnless().isEmpty() && !files.contains(paths.get(i))) {
                return false;
            }
        }
        return true;
    }

    private ArchitectureProfile architecture(String id) {
        return catalog.architectures().stream().filter(profile -> profile.id().equals(id)).findFirst().orElseThrow();
    }

    /** Checked only when the architecture is being chosen: a project that already follows it made that decision. */
    private static Optional<CrudPlan.Cost> cost(ArchitectureProfile architecture, Optional<ArchitectureProfile> detected,
                                                Optional<ProjectContext> context) {
        boolean followed = detected.filter(project -> project.id().equals(architecture.id())).isPresent();
        if (followed || architecture.cost().complexity() == Complexity.LOW) {
            return Optional.empty();
        }
        return Optional.of(new CrudPlan.Cost(architecture.justifiedBy(context)));
    }

    /**
     * The templates of the project's stack: those whose build group the build declares, or, if
     * none does, those of the default stack. A Quarkus project never gets a Spring CRUD.
     */
    private List<Scaffold> scaffolds(ProjectModel project) {
        Set<String> groups = project.build().stream()
                .flatMap(build -> build.allDependencies().stream())
                .map(Dependency::groupId)
                .collect(Collectors.toSet());
        List<Scaffold> all = catalog.scaffolds();
        List<Scaffold> stack = all.stream().filter(scaffold -> scaffold.buildGroup().filter(groups::contains).isPresent()).toList();
        return stack.isEmpty() ? all.stream().filter(scaffold -> scaffold.buildGroup().isEmpty()).toList() : stack;
    }

    private static Scaffold scaffold(CrudRequest request, Optional<ArchitectureProfile> detected, List<Scaffold> scaffolds) {
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

    /** On a class or on a method: Quarkus REST's {@code @ServerExceptionMapper} annotates methods. */
    private static boolean annotated(ProjectModel project, String annotation) {
        String simpleName = annotation.startsWith("@") ? annotation.substring(1) : annotation;
        return project.classes().stream().anyMatch(unit -> unit.isAnnotatedWith(simpleName)
                || unit.methods().stream().anyMatch(method -> method.annotations().stream()
                        .anyMatch(methodAnnotation -> methodAnnotation.simpleName().equals(simpleName))));
    }

    private static List<List<String>> missingDependencies(Scaffold scaffold, ProjectModel project) {
        Set<String> present = project.build().stream()
                .flatMap(build -> build.allDependencies().stream())
                .map(Dependency::artifactId)
                .collect(Collectors.toSet());
        return scaffold.requiredDependencies().stream()
                .filter(alternatives -> alternatives.stream().noneMatch(present::contains))
                .toList();
    }
}
