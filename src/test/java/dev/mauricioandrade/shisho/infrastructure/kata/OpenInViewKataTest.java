package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OpenInViewKataTest {

    private static final String DATA_JPA = "org.springframework.boot:spring-boot-starter-data-jpa";

    private final OpenInViewKata kata = new OpenInViewKata();

    @Test
    void flagsAJpaApplicationThatNeverSaysAndPointsAtItsMainSettingsFile() {
        List<Lesson> lessons = KataTestSupport.evaluateSettings(kata, Map.of(
                "src/main/resources/application-prod.yml", "server:\n  port: 80\n",
                "src/main/resources/application.yml", "spring:\n  application:\n    name: loja\n"),
                KataTestSupport.build("pom.xml", DATA_JPA));

        assertThat(lessons).singleElement().satisfies(lesson -> {
            assertThat(lesson.file()).isEqualTo(Path.of("src/main/resources/application.yml"));
            assertThat(lesson.line()).isZero();
        });
    }

    @Test
    void withoutSettingsTheLessonGoesToTheBuild() {
        assertThat(KataTestSupport.evaluateSettings(kata, Map.of(), KataTestSupport.build("pom.xml", DATA_JPA)))
                .extracting(Lesson::file).containsExactly(Path.of("pom.xml"));
    }

    @Test
    void eitherValueWrittenIsAChoice() {
        assertThat(KataTestSupport.evaluateSettings(kata, "spring.jpa.open-in-view=true\n", DATA_JPA)).isEmpty();
        assertThat(KataTestSupport.evaluateSettings(kata, Map.of("src/main/resources/application-prod.properties",
                "spring.jpa.openInView=false\n"), KataTestSupport.build("pom.xml", DATA_JPA))).isEmpty();
    }

    @Test
    void leavesAloneWhatItCannotSee() {
        assertThat(KataTestSupport.evaluateSettings(kata, "spring.config.import=optional:configserver:http://config:8888\n", DATA_JPA))
                .as("settings from a config server").isEmpty();
        assertThat(KataTestSupport.evaluateSettings(kata, "spring.application.name=loja\n",
                "org.springframework.boot:spring-boot-starter-jdbc")).as("no JPA").isEmpty();
    }

    @Test
    void aParentDeclaringTheDependencyIsDecidedByItsModules() {
        BuildInfo system = KataTestSupport.build("eladmin-system/pom.xml");
        BuildInfo root = new BuildInfo(Path.of("pom.xml"), Optional.of(new JavaVersion(25)),
                KataTestSupport.build("pom.xml", DATA_JPA).dependencies(), List.of(system));

        assertThat(KataTestSupport.evaluateSettings(kata, Map.of(
                "eladmin-system/src/main/resources/config/application.yml", "spring:\n  jpa:\n    open-in-view: true\n"), root)).isEmpty();
    }

    @Test
    void decidesEachModuleWithItsOwnSettings() {
        BuildInfo customers = KataTestSupport.build("customers/pom.xml", DATA_JPA);
        BuildInfo vets = KataTestSupport.build("vets/pom.xml", DATA_JPA);
        BuildInfo root = new BuildInfo(Path.of("pom.xml"), Optional.of(new JavaVersion(25)), List.of(), List.of(customers, vets));

        List<Lesson> lessons = KataTestSupport.evaluateSettings(kata, Map.of(
                "customers/src/main/resources/application.properties", "spring.jpa.open-in-view=false\n",
                "vets/src/main/resources/application.properties", "server.port=8083\n",
                "src/main/resources/application.properties", "spring.jpa.open-in-view=false\n"), root);

        assertThat(lessons).extracting(Lesson::file).containsExactly(Path.of("vets/src/main/resources/application.properties"));
    }
}
