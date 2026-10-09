package dev.mauricioandrade.shisho.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PackagePatternTest {

    @Test
    void matchesConsecutiveSegmentsAnywhereInThePackage() {
        PackagePattern pattern = PackagePattern.parse("domain/model");

        assertThat(pattern.matches("dev.app.domain.model")).isTrue();
        assertThat(pattern.matches("dev.app.domain.model.user")).isTrue();
        assertThat(pattern.matches("dev.app.domain.port.model")).isFalse();
        assertThat(pattern.matches("dev.app.domainmodel")).isFalse();
        assertThat(pattern.hasPlaceholder()).isFalse();
    }

    @Test
    void bindsPlaceholdersToTheSegmentTheyStandFor() {
        PackagePattern pattern = PackagePattern.parse("<feature>/controller");

        assertThat(pattern.hasPlaceholder()).isTrue();
        assertThat(pattern.bindings("com.shop.order.controller")).contains(List.of("order"));
        assertThat(pattern.bindings("controller")).isEmpty();
        assertThat(pattern.bindings("com.shop.order.service")).isEmpty();
    }

    @Test
    void matchesAClassSegmentBySuffixOnlyAtTheEndOfAQualifiedClassName() {
        PackagePattern pattern = PackagePattern.parse("<feature>/*Controller");

        assertThat(pattern.isClassPattern()).isTrue();
        assertThat(pattern.bindings("com.shop.order.OrderController")).contains(List.of("order"));
        assertThat(pattern.bindings("com.shop.order.controller.OrderController")).contains(List.of("controller"));
        assertThat(pattern.bindings("com.shop.order.OrderService")).isEmpty();
        assertThat(pattern.bindings("com.shop.order.Controller")).isEmpty();
        assertThat(pattern.bindings("com.shop.OrderController.inner")).isEmpty();
        assertThat(PackagePattern.parse("domain/model").isClassPattern()).isFalse();
    }
}
