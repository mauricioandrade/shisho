package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class HardcodedSecretKataTest {

    private final HardcodedSecretKata kata = new HardcodedSecretKata();

    @Test
    void flagsALiteralByTheLastWordOfTheKeyWithoutRepeatingTheValue() {
        List<Lesson> lessons = KataTestSupport.evaluateSettings(kata, """
                spring.datasource.password=s3nh4
                jwt.secret=abc
                spring.datasource.druid.stat-view-servlet.login-password=druid
                alipay.appPrivateKey=MIIB
                amz.s3.secretKey=xyz
                github.api_key=123
                quarkus.oidc.credentials.secret=secret
                %prod.quarkus.datasource.password=quarkus_test
                """);

        assertThat(lessons).extracting(Lesson::line, Lesson::arguments).containsExactly(
                tuple(1, List.of("spring.datasource.password")),
                tuple(2, List.of("jwt.secret")),
                tuple(3, List.of("spring.datasource.druid.stat-view-servlet.login-password")),
                tuple(4, List.of("alipay.appPrivateKey")),
                tuple(5, List.of("amz.s3.secretKey")),
                tuple(6, List.of("github.api_key")),
                tuple(7, List.of("quarkus.oidc.credentials.secret")),
                tuple(8, List.of("quarkus.datasource.password")));
        assertThat(lessons).flatExtracting(Lesson::arguments).doesNotContain("s3nh4", "abc", "MIIB", "quarkus_test");
    }

    @Test
    void aKeyThatOnlyMentionsASecretWordIsNotASecret() {
        assertThat(KataTestSupport.evaluateSettings(kata, """
                jhipster.security.authentication.jwt.token-validity-in-seconds=86400
                spring.security.password-encoder=bcrypt
                app.secret-rotation.enabled=true
                management.endpoint.health.show-details=always
                """)).isEmpty();
    }

    @Test
    void acceptsPlaceholdersEmptyValuesAndTheDevelopmentProfiles() {
        assertThat(KataTestSupport.evaluateSettings(kata, Map.of(
                "src/main/resources/application.properties", """
                        spring.datasource.password=${DB_PASSWORD}
                        jwt.secret=${JWT_SECRET:only-for-my-machine}
                        spring.datasource.password=
                        mail.password.required=true
                        %dev.quarkus.datasource.password=quarkus
                        %test.quarkus.datasource.password=quarkus
                        """,
                "src/main/resources/application-local.yml", "spring:\n  datasource:\n    password: root\n"),
                KataTestSupport.build("pom.xml"))).isEmpty();
    }
}
