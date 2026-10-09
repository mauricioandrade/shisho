package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class PreferConfigurationPropertiesKataTest {

    private final PreferConfigurationPropertiesKata kata = new PreferConfigurationPropertiesKata();

    @Test
    void flagsThreeValuesWithTheSamePrefixWhereverTheyAre() {
        assertThat(evaluate(kata, """
                @Service
                class MailService {
                    @Value("${mail.host:localhost}")
                    private String host;

                    MailService(@Value("${mail.port}") int port) {}

                    @Value("${mail.from}")
                    void setFrom(String from) {}
                }
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(3);
            assertThat(lesson.arguments()).containsExactly("mail", 3);
            assertThat(lesson.messageKey()).isEqualTo("kata.prefer-configuration-properties.lesson");
        });
    }

    @Test
    void groupsByTheWholePrefix() {
        assertThat(evaluate(kata, """
                @Service
                class Clients {
                    Clients(@Value("${clients.billing.url}") String billingUrl,
                            @Value("${clients.billing.timeout}") int billingTimeout,
                            @Value("${clients.billing.retries}") int billingRetries,
                            @Value("${clients.stock.url}") String stockUrl,
                            @Value("${clients.stock.timeout}") int stockTimeout) {}
                }
                """)).singleElement().satisfies(lesson ->
                assertThat(lesson.arguments()).containsExactly("clients.billing", 3));
    }

    @Test
    void acceptsFewOrUnrelatedValuesAndExpressions() {
        assertThat(evaluate(kata, """
                @Service
                class MailService {
                    MailService(@Value("${mail.host}") String host,
                                @Value("${mail.port}") int port,
                                @Value("${timeout}") int timeout,
                                @Value("${server.port}") int serverPort,
                                @Value("#{systemProperties['user.home']}") String home) {}
                }
                """)).isEmpty();
    }
}
