package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ArchitectureMatch;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.AvoidedDependency;
import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Annotation;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Import;
import dev.mauricioandrade.shisho.domain.model.Concept;
import dev.mauricioandrade.shisho.domain.model.Dependency;
import dev.mauricioandrade.shisho.domain.model.JavaFeature;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.KataGuide;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Scaffold;
import dev.mauricioandrade.shisho.domain.port.out.CatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ClassifyArchitectureServiceTest {

    private static final ArchitectureProfile LAYERED = architecture("layered", 3, List.of("controller", "service", "repository", "dto"), List.of());
    private static final ArchitectureProfile BY_FEATURE = architecture("by-feature", 2,
            List.of("<feature>/*Controller", "<feature>/*Service", "<feature>/controller", "<feature>/service"), List.of());
    private static final ArchitectureProfile EVENTS = architecture("events", 2, List.of("events"),
            List.of("@KafkaListener", "spring-kafka", "org.axonframework", "broker_externo_no_mesmo_servico"));

    private final ClassifyArchitectureService service = new ClassifyArchitectureService(new CatalogRepository() {
        @Override
        public List<ArchitectureProfile> architectures() {
            return List.of(LAYERED, BY_FEATURE, EVENTS);
        }

        @Override
        public List<KataGuide> kataGuides() {
            return List.of();
        }

        @Override
        public List<JavaFeature> javaFeatures() {
            return List.of();
        }

        @Override
        public List<AvoidedDependency> avoidedDependencies() {
            return List.of();
        }

        @Override
        public List<Scaffold> scaffolds() {
            return List.of();
        }

        @Override
        public List<Concept> concepts() {
            return List.of();
        }
    }, List.of());

    @Test
    void picksTheArchitectureWithTheHighestShareOfSignals() {
        ArchitectureMatch match = classify(project("app.controller", "app.service", "app.repository")).orElseThrow();

        assertThat(match.architecture()).isEqualTo(LAYERED);
        assertThat(match.signals()).containsExactly("controller", "service", "repository");
        assertThat(match.confidence()).isEqualTo(75);
    }

    @Test
    void needsTheMinimumNumberOfSignals() {
        assertThat(classify(project("app.controller", "app.service"))).isEmpty();
    }

    @Test
    void placeholdersNeedTwoDifferentBindings() {
        assertThat(classify(project("shop.order.controller", "shop.order.service"))).isEmpty();

        ArchitectureMatch match = classify(project(
                "shop.order.controller", "shop.order.service", "shop.user.controller", "shop.user.service")).orElseThrow();
        assertThat(match.architecture()).isEqualTo(BY_FEATURE);
        assertThat(match.signals()).containsExactly("<feature>/controller", "<feature>/service");
    }

    @Test
    void recognizesFlatFeaturePackagesByTheirClassNames() {
        assertThat(classify(classes("shop.order.OrderController", "shop.order.OrderService"))).isEmpty();

        ArchitectureMatch match = classify(classes("shop.order.OrderController", "shop.order.OrderService",
                "shop.user.UserController", "shop.user.UserService")).orElseThrow();
        assertThat(match.architecture()).isEqualTo(BY_FEATURE);
        assertThat(match.signals()).containsExactly("<feature>/*Controller", "<feature>/*Service");
    }

    @Test
    void classPatternsDoNotMistakeLayersForFeatures() {
        assertThat(classify(classes("shop.controller.OrderController", "shop.controller.UserController",
                "shop.service.OrderService", "shop.service.UserService", "shop.repository.OrderRepository")))
                .hasValueSatisfying(match -> assertThat(match.architecture()).isEqualTo(LAYERED));
    }

    @Test
    void recognizesAnnotationsImportsAndDependenciesButNotDetectorIds() {
        ClassUnit listener = new ClassUnit(Path.of("Listener.java"), "app.events", "Listener", ClassUnit.Kind.CLASS, 1,
                List.of(), List.of(new Import("org.axonframework.commandhandling.CommandHandler", false, false, false, 2)),
                List.of(), List.of(),
                List.of(new ClassUnit.Method("on", "void", List.of(new Annotation("KafkaListener", 3)), List.of(), 3)),
                List.of());
        BuildInfo build = new BuildInfo(Path.of("pom.xml"), Optional.empty(),
                List.of(new Dependency("org.springframework.kafka", "spring-kafka")));

        ArchitectureMatch match = service.classify(
                new ProjectModel(new JavaVersion(21), Optional.of(build), List.of(listener)), Optional.empty()).orElseThrow();

        assertThat(match.architecture()).isEqualTo(EVENTS);
        assertThat(match.signals()).containsExactly("events", "@KafkaListener", "spring-kafka", "org.axonframework");
        assertThat(match.confidence()).isEqualTo(80);
    }

    @Test
    void theCatalogRecognizesPackagesNamedInPortuguese() {
        ClassifyArchitectureService bundled = new ClassifyArchitectureService(MarkdownCatalogRepository.bundled(), Detectors.all());

        assertThat(bundled.classify(project("com.loja.dominio", "com.loja.porta.entrada", "com.loja.porta.saida",
                "com.loja.adaptador.entrada.web", "com.loja.adaptador.saida.persistencia"), Optional.empty()))
                .hasValueSatisfying(match -> {
                    assertThat(match.architecture().id()).isEqualTo("hexagonal");
                    assertThat(match.signals()).as("shown by the first alternative")
                            .containsExactly("domain", "port", "adapter", "port/in", "port/out", "adapter/in", "adapter/out");
                });
        assertThat(bundled.classify(project("com.loja.controlador", "com.loja.servico", "com.loja.repositorio", "com.loja.modelo"),
                Optional.empty())).hasValueSatisfying(match -> assertThat(match.architecture().id()).isEqualTo("layered"));
    }

    private Optional<ArchitectureMatch> classify(ProjectModel project) {
        return service.classify(project, Optional.empty());
    }

    private static ProjectModel project(String... packages) {
        List<ClassUnit> classes = Arrays.stream(packages)
                .map(name -> new ClassUnit(Path.of(name + "/C.java"), name, "C", ClassUnit.Kind.CLASS, 1,
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                .toList();
        return new ProjectModel(new JavaVersion(21), Optional.empty(), classes);
    }

    private static ProjectModel classes(String... qualifiedNames) {
        List<ClassUnit> classes = Arrays.stream(qualifiedNames)
                .map(name -> new ClassUnit(Path.of(name.replace('.', '/') + ".java"), name.substring(0, name.lastIndexOf('.')),
                        name.substring(name.lastIndexOf('.') + 1), ClassUnit.Kind.CLASS, 1,
                        List.of(), List.of(), List.of(), List.of(), List.of(), List.of()))
                .toList();
        return new ProjectModel(new JavaVersion(21), Optional.empty(), classes);
    }

    private static ArchitectureProfile architecture(String id, int minimumSignals, List<String> packages, List<String> markers) {
        return new ArchitectureProfile(id, id, Family.SIMPLE, new Signature(packages, markers, minimumSignals), List.of(),
                new Cost(new ClassRange(4, 6), Complexity.LOW), List.of(), List.of(), List.of(), List.of(),
                Optional.empty(), List.of(), "");
    }
}
