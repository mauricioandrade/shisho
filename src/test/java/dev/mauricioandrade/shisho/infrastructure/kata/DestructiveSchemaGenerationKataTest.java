package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class DestructiveSchemaGenerationKataTest {

    private final DestructiveSchemaGenerationKata kata = new DestructiveSchemaGenerationKata();

    @Test
    void flagsHibernateDroppingTheSchemaInEveryProfile() {
        List<Lesson> lessons = KataTestSupport.evaluateSettings(kata, """
                spring.jpa.hibernate.ddl-auto=create
                spring.jpa.properties.hibernate.hbm2ddl.auto=CREATE-DROP
                spring.jpa.properties.jakarta.persistence.schema-generation.database.action=drop-and-create
                """);

        assertThat(lessons).extracting(Lesson::line, Lesson::messageKey, Lesson::arguments).containsExactly(
                tuple(1, "kata.destructive-schema-generation.lesson", List.of("spring.jpa.hibernate.ddl-auto", "create")),
                tuple(2, "kata.destructive-schema-generation.lesson", List.of("spring.jpa.properties.hibernate.hbm2ddl.auto", "CREATE-DROP")),
                tuple(3, "kata.destructive-schema-generation.lesson",
                        List.of("spring.jpa.properties.jakarta.persistence.schema-generation.database.action", "drop-and-create")));
    }

    @Test
    void readsTheRelaxedSpringNamesToo() {
        assertThat(KataTestSupport.evaluateSettings(kata, "spring.jpa.hibernate.ddlAuto=create-drop\n")).hasSize(1);
    }

    @Test
    void acceptsWhatDoesNotDropTheSchema() {
        assertThat(KataTestSupport.evaluateSettings(kata, """
                spring.jpa.hibernate.ddl-auto=update
                spring.jpa.hibernate.ddl-auto=validate
                spring.jpa.hibernate.ddl-auto=none
                quarkus.hibernate-orm.schema-management.strategy=create
                quarkus.hibernate-orm.database.generation=update
                """)).isEmpty();
    }

    @Test
    void leavesTheDevelopmentAndTestProfilesAlone() {
        List<Lesson> lessons = KataTestSupport.evaluateSettings(kata, Map.of(
                "src/main/resources/application-dev.properties", "spring.jpa.hibernate.ddl-auto=create-drop\n",
                "src/main/resources/application-test.yml", "spring:\n  jpa:\n    hibernate:\n      ddl-auto: create-drop\n",
                "src/main/resources/application-prod.yml", "spring:\n  jpa:\n    hibernate:\n      ddl-auto: create-drop\n",
                "src/main/resources/application.properties", """
                        %dev.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        %test.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        %prod.quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                        """), KataTestSupport.build("pom.xml"));

        assertThat(lessons).extracting(lesson -> lesson.file().toString(), Lesson::line).containsExactlyInAnyOrder(
                tuple(Path.of("src/main/resources/application-prod.yml").toString(), 4),
                tuple(Path.of("src/main/resources/application.properties").toString(), 3));
    }

    @Test
    void aQuarkusSettingThatProductionReplacesIsForTheOtherProfiles() {
        assertThat(KataTestSupport.evaluateSettings(kata, """
                quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                %prod.quarkus.hibernate-orm.schema-management.strategy=create
                """)).isEmpty();
    }

    @Test
    void teachesTheQuarkusProfilePrefixToAQuarkusSetting() {
        List<Lesson> lessons = KataTestSupport.evaluateSettings(kata, """
                quarkus.hibernate-orm.schema-management.strategy=drop-and-create
                quarkus.hibernate-orm."orders".database.generation=drop-and-create
                """);

        assertThat(lessons).extracting(Lesson::messageKey).containsOnly("kata.destructive-schema-generation.lesson.quarkus");
        assertThat(lessons).hasSize(2);
    }
}
