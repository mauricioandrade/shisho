package dev.mauricioandrade.shisho.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class JavaVersionTest {

    @ParameterizedTest
    @CsvSource({"21, 21", "25, 25", "' 17 ', 17", "1.8, 8", "8, 8"})
    void parsesFeatureReleases(String text, int feature) {
        assertThat(JavaVersion.parse(text)).contains(new JavaVersion(feature));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "21.0.2", "0", "1.", "${java.version}"})
    void rejectsAnythingElse(String text) {
        assertThat(JavaVersion.parse(text)).isEmpty();
    }

    @Test
    void comparesByFeatureRelease() {
        assertThat(new JavaVersion(25).isAtLeast(new JavaVersion(21))).isTrue();
        assertThat(new JavaVersion(21).isAtLeast(new JavaVersion(21))).isTrue();
        assertThat(new JavaVersion(17).isAtLeast(new JavaVersion(21))).isFalse();
    }
}
