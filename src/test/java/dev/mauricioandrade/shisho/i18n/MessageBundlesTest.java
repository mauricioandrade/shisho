package dev.mauricioandrade.shisho.i18n;

import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Kata;
import dev.mauricioandrade.shisho.infrastructure.detector.Detectors;
import dev.mauricioandrade.shisho.infrastructure.kata.Katas;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class MessageBundlesTest {

    @Test
    void portugueseAndEnglishBundlesHaveTheSameKeys() throws IOException {
        assertThat(load("messages_pt_BR.properties").stringPropertyNames())
                .containsExactlyInAnyOrderElementsOf(load("messages.properties").stringPropertyNames());
    }

    @Test
    void everyKataHasALessonMessage() throws IOException {
        Properties messages = load("messages_pt_BR.properties");

        assertThat(Katas.all()).extracting(Kata::id).allSatisfy(id ->
                assertThat(messages).containsKey("kata." + id + ".lesson"));
    }

    @Test
    void everyDetectorHasAnEvidenceMessage() throws IOException {
        Properties messages = load("messages_pt_BR.properties");

        assertThat(Detectors.all()).extracting(Detector::id).allSatisfy(id ->
                assertThat(messages).containsKey("detector." + id));
    }

    private static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = MessageBundlesTest.class.getResourceAsStream("/" + resource)) {
            assertThat(stream).as(resource).isNotNull();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
