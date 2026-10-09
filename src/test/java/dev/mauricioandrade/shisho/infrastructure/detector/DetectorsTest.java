package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.infrastructure.catalog.MarkdownCatalogRepository;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DetectorsTest {

    @Test
    void everyDetectorMeasuresASignalTheCatalogNames() {
        Set<String> catalogSignals = MarkdownCatalogRepository.bundled().architectures().stream()
                .flatMap(architecture -> Stream.concat(
                        architecture.overengineeringSignals().stream(), architecture.underengineeringSignals().stream()))
                .collect(Collectors.toSet());

        assertThat(Detectors.all()).extracting(Detector::id).doesNotHaveDuplicates().allSatisfy(id ->
                assertThat(catalogSignals).contains(id));
    }
}
