package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ValueOutsideConstructorKataTest {

    private final ValueOutsideConstructorKata kata = new ValueOutsideConstructorKata();

    @Test
    void flagsValueOnFieldsMethodsAndMethodParameters() {
        assertThat(evaluate(kata, """
                @Component
                class Mailer {
                    @Value("${mail.host}")
                    private String host;

                    @Value("${mail.port}")
                    void setPort(int port) {}

                    void setTimeout(@Value("${mail.timeout}") int timeout) {}
                }
                """)).extracting(Lesson::line, lesson -> lesson.arguments().getFirst()).containsExactly(
                tuple(3, "host"),
                tuple(6, "setPort"),
                tuple(9, "timeout"));
    }

    @Test
    void acceptsValueOnConstructorAndRecordComponents() {
        assertThat(evaluate(kata, """
                @Component
                class Mailer {
                    private final String host;

                    Mailer(@Value("${mail.host}") String host) {
                        this.host = host;
                    }
                }

                record MailSettings(@Value("${mail.host}") String host) {}

                @lombok.Value
                class Address {
                    String street;
                }
                """)).isEmpty();
    }
}
