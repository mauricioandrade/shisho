package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationPropertiesNotRecordKataTest {

    private final ConfigurationPropertiesNotRecordKata kata = new ConfigurationPropertiesNotRecordKata();

    @Test
    void flagsClassesBoundThroughSetters() {
        assertThat(evaluate(kata, """
                @Validated
                @ConfigurationProperties(prefix = "mail")
                class MailProperties {
                    private String host;

                    void setHost(String host) { this.host = host; }
                }
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(2);
            assertThat(lesson.arguments()).containsExactly("MailProperties");
            assertThat(lesson.messageKey()).isEqualTo("kata.configuration-properties-not-record.lesson");
        });
    }

    @Test
    void acceptsRecordsAndBeanMethods() {
        assertThat(evaluate(kata, """
                @ConfigurationProperties("mail")
                record MailProperties(String host, int port) {}

                @Configuration
                class ClientConfiguration {
                    @Bean
                    @ConfigurationProperties("client")
                    ClientSettings clientSettings() { return new ClientSettings(); }
                }
                """)).isEmpty();
    }
}
