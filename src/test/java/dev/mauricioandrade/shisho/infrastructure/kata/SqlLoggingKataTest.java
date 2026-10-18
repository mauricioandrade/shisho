package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SqlLoggingKataTest {

    private final SqlLoggingKata kata = new SqlLoggingKata();

    @Test
    void flagsTheSwitchesAndTheLoggerAtDebug() {
        List<Lesson> lessons = KataTestSupport.evaluateSettings(kata, Map.of("src/main/resources/application.yml", """
                spring:
                  jpa:
                    show-sql: true
                    properties:
                      hibernate:
                        show_sql: true
                logging:
                  level:
                    org.hibernate.SQL: DEBUG
                """), KataTestSupport.build("pom.xml"));

        assertThat(lessons).extracting(Lesson::line, Lesson::arguments).containsExactly(
                tuple(3, List.of("spring.jpa.show-sql")),
                tuple(6, List.of("spring.jpa.properties.hibernate.show_sql")),
                tuple(9, List.of("logging.level.org.hibernate.SQL")));
    }

    @Test
    void acceptsTheSqlOffAQuieterLoggerAndTheDevelopmentProfile() {
        assertThat(KataTestSupport.evaluateSettings(kata, Map.of(
                "src/main/resources/application.properties", """
                        spring.jpa.show-sql=false
                        logging.level.org.hibernate.SQL=info
                        logging.level.org.hibernate=debug
                        %dev.quarkus.hibernate-orm.log.sql=true
                        """,
                "src/main/resources/application-dev.properties", "spring.jpa.show-sql=true\n"), KataTestSupport.build("pom.xml"))).isEmpty();
    }

    @Test
    void teachesTheQuarkusProfilePrefixUnlessProductionTurnsItOff() {
        assertThat(KataTestSupport.evaluateSettings(kata, "quarkus.hibernate-orm.log.sql=true\n"))
                .extracting(Lesson::messageKey).containsExactly("kata.sql-logging.lesson.quarkus");
        assertThat(KataTestSupport.evaluateSettings(kata, """
                quarkus.hibernate-orm.log.sql=true
                %prod.quarkus.hibernate-orm.log.sql=false
                """)).isEmpty();
    }
}
