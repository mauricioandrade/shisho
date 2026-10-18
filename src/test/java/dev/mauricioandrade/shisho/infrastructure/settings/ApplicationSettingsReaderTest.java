package dev.mauricioandrade.shisho.infrastructure.settings;

import dev.mauricioandrade.shisho.domain.model.SettingsFile;
import dev.mauricioandrade.shisho.domain.model.SettingsFile.Setting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ApplicationSettingsReaderTest {

    private final ApplicationSettingsReader reader = new ApplicationSettingsReader();

    @TempDir
    Path project;

    @Test
    void readsAPropertiesFileTheWayPropertiesDoes() throws IOException {
        write("src/main/resources/application.properties", """
                # a comment
                ! another
                server.port=8080
                spring.application.name: loja
                greeting   Olá \\
                    mundo
                path=C:\\\\temp
                empty=
                """);

        assertThat(settings("src/main/resources/application.properties")).containsExactly(
                new Setting("server.port", "8080", 3, List.of()),
                new Setting("spring.application.name", "loja", 4, List.of()),
                new Setting("greeting", "Olá mundo", 5, List.of()),
                new Setting("path", "C:\\temp", 7, List.of()),
                new Setting("empty", "", 8, List.of()));
    }

    @Test
    void flattensYamlWithTheLineOfEachKey() throws IOException {
        write("src/main/resources/application.yml", """
                spring:
                  jpa:
                    open-in-view: false
                  datasource:
                    password:
                logging:
                  level:
                    "[org.hibernate.SQL]": debug
                hosts:
                  - a
                  - b
                """);

        assertThat(settings("src/main/resources/application.yml")).containsExactly(
                new Setting("spring.jpa.open-in-view", "false", 3, List.of()),
                new Setting("spring.datasource.password", "", 5, List.of()),
                new Setting("logging.level.org.hibernate.SQL", "debug", 8, List.of()),
                new Setting("hosts[0]", "a", 10, List.of()),
                new Setting("hosts[1]", "b", 11, List.of()));
    }

    @Test
    void takesTheProfilesFromTheFileNameTheQuarkusPrefixAndTheDocument() throws IOException {
        write("src/main/resources/config/application-prod.yml", "server.port: 80\n");
        write("src/main/resources/application.properties", """
                %dev,test.quarkus.hibernate-orm.log.sql=true
                #---
                spring.config.activate.on-profile=local
                spring.jpa.show-sql=true
                """);
        write("src/main/resources/application.yaml", """
                a: 1
                ---
                spring:
                  config:
                    activate:
                      on-profile: docker | !dev
                b: 2
                """);

        assertThat(settings("src/main/resources/config/application-prod.yml"))
                .containsExactly(new Setting("server.port", "80", 1, List.of("prod")));
        assertThat(settings("src/main/resources/application.properties")).containsExactly(
                new Setting("quarkus.hibernate-orm.log.sql", "true", 1, List.of("dev", "test")),
                new Setting("spring.config.activate.on-profile", "local", 3, List.of("local")),
                new Setting("spring.jpa.show-sql", "true", 4, List.of("local")));
        assertThat(settings("src/main/resources/application.yaml")).extracting(Setting::key, Setting::profiles).containsExactly(
                tuple("a", List.of()),
                tuple("spring.config.activate.on-profile", List.of("docker", "!dev")),
                tuple("b", List.of("docker", "!dev")));
    }

    @Test
    void findsTheFilesOfEveryModuleButNotTestsOrBuildOutput() throws IOException {
        write("customers/src/main/resources/application.yml", "a: 1\n");
        write("src/main/resources/application.properties", "a=1\n");
        write("src/main/resources/bootstrap.yml", "a: 1\n");
        write("src/main/resources/static/application.properties", "a=1\n");
        write("src/test/resources/application.properties", "a=1\n");
        write("target/classes/application.properties", "a=1\n");

        assertThat(reader.read(project)).extracting(SettingsFile::file).containsExactly(
                Path.of("customers/src/main/resources/application.yml"),
                Path.of("src/main/resources/application.properties"));
    }

    @Test
    void leavesOutAFileThatIsNotYaml() throws IOException {
        write("src/main/resources/application.yml", "a: [1\n");
        write("src/main/resources/application.properties", "a=1\n");

        assertThat(reader.read(project)).extracting(SettingsFile::file)
                .containsExactly(Path.of("src/main/resources/application.properties"));
    }

    private List<Setting> settings(String file) {
        return reader.read(project).stream().filter(found -> found.file().equals(Path.of(file)))
                .findFirst().orElseThrow().settings();
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = project.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}
