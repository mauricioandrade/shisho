package dev.mauricioandrade.shisho.infrastructure.detector;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static dev.mauricioandrade.shisho.infrastructure.detector.DetectorTestSupport.project;
import static org.assertj.core.api.Assertions.assertThat;

class CrowdedServicePackageDetectorTest {

    private final CrowdedServicePackageDetector detector = new CrowdedServicePackageDetector();

    @Test
    void findsAServicePackageWithMoreThanFifteenClasses() {
        assertThat(detector.detect(project(services(16)))).hasValueSatisfying(evidence ->
                assertThat(evidence.arguments()).containsExactly("shop.service", 16L));
    }

    @Test
    void acceptsFifteen() {
        assertThat(detector.detect(project(services(15)))).isEmpty();
    }

    private static Map<String, String> services(int count) {
        Map<String, String> sources = new HashMap<>();
        for (int i = 0; i < count; i++) {
            sources.put("shop/service/Service" + i + ".java", "package shop.service; class Service" + i + " {}");
        }
        sources.put("shop/web/Controller.java", "package shop.web; class Controller {}");
        return sources;
    }
}
