package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.DependencyRule;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.ClassUnit.Import;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.Severity;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DependencyRulesTest {

    private static final ArchitectureProfile CLEAN = new ArchitectureProfile("clean", "Clean Architecture", Family.DOMAIN_ISOLATION,
            new Signature(List.of("domain"), List.of(), 1),
            List.of(
                    new DependencyRule("domain", List.of("application", "infrastructure", "org.springframework")),
                    new DependencyRule("<modulo>", List.of("<outro-modulo>/internal")),
                    new DependencyRule("application", List.of("banco:infrastructure"))),
            new Cost(new ClassRange(7, 12), Complexity.HIGH), List.of(), List.of(), List.of(), List.of(),
            Optional.empty(), List.of(), "");

    private static final Set<String> PACKAGES = Set.of(
            "shop.domain", "shop.domain.model", "shop.application", "shop.infrastructure.jpa", "shop.adapter");

    @Test
    void flagsImportsOfForbiddenProjectPackagesAndLibraries() {
        ClassUnit order = unit("shop.domain.model", "Order",
                "shop.domain.model.OrderId",
                "shop.application.PlaceOrder",
                "shop.infrastructure.jpa.OrderEntity",
                "org.springframework.stereotype.Component",
                "org.springframeworkish.Thing",
                "java.util.List");

        List<Lesson> violations = new DependencyRules(CLEAN, PACKAGES).violations(List.of(order));

        assertThat(violations)
                .extracting(Lesson::line, lesson -> lesson.arguments().get(2), lesson -> lesson.arguments().get(3))
                .containsExactly(
                        tuple(3, "shop.application.PlaceOrder", "application"),
                        tuple(4, "shop.infrastructure.jpa.OrderEntity", "infrastructure"),
                        tuple(5, "org.springframework.stereotype.Component", "org.springframework"));
        assertThat(violations.getFirst()).satisfies(lesson -> {
            assertThat(lesson.kataId()).isEqualTo("dependency-rule");
            assertThat(lesson.severity()).isEqualTo(Severity.MEDIA);
            assertThat(lesson.messageKey()).isEqualTo("architecture.dependency-rule.lesson");
            assertThat(lesson.arguments()).containsExactly(
                    "Order", "domain", "shop.application.PlaceOrder", "application", "Clean Architecture");
        });
    }

    @Test
    void onlyChecksClassesInTheRestrictedPackage() {
        ClassUnit adapter = unit("shop.adapter", "OrderController", "shop.application.PlaceOrder", "org.springframework.web.bind.annotation.RestController");

        assertThat(new DependencyRules(CLEAN, PACKAGES).violations(List.of(adapter))).isEmpty();
    }

    @Test
    void anImportOutsideTheProjectIsNotAProjectPackage() {
        ClassUnit order = unit("shop.domain", "Order", "com.vendor.application.Client");

        assertThat(new DependencyRules(CLEAN, PACKAGES).violations(List.of(order))).isEmpty();
    }

    /** Imports start on line 2, one per line. */
    private static ClassUnit unit(String packageName, String name, String... imports) {
        List<Import> declarations = Arrays.stream(imports)
                .map(imported -> new Import(imported, false, false, false, 2 + Arrays.asList(imports).indexOf(imported)))
                .toList();
        return new ClassUnit(Path.of(name + ".java"), packageName, name, ClassUnit.Kind.CLASS, 1,
                List.of(), declarations, List.of(), List.of(), List.of(), List.of());
    }
}
