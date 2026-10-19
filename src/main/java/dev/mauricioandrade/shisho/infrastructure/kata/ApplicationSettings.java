package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.SettingsFile;
import dev.mauricioandrade.shisho.domain.model.SettingsFile.Setting;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The application's settings as the katas read them. Keys are compared the way Spring's relaxed
 * binding does ({@code open-in-view}, {@code openInView} and {@code open_in_view} are one key).
 * A setting only for development or tests ({@code application-dev.yml}, {@code %test.}) is left to
 * the katas that care about production.
 */
final class ApplicationSettings {

    private static final Set<String> DEVELOPMENT_PROFILES = Set.of("dev", "development", "local", "test");
    private static final String QUARKUS_PRODUCTION = "prod";
    private static final Path SRC = Path.of("src");
    private static final Path MAIN = Path.of("main");
    private static final Path RESOURCES = Path.of("resources");

    private ApplicationSettings() {
    }

    /** A setting and the file it is in. */
    record Found(SettingsFile file, Setting setting) {

        String key() {
            return setting.key();
        }

        /** The value trimmed and in lower case, for comparing with {@code true} or {@code create-drop}. */
        String value() {
            return setting.value().trim().toLowerCase(Locale.ROOT);
        }

        boolean named(String key) {
            return canonical(setting.key()).equals(canonical(key));
        }

        /** The setting applies only to development or test profiles. */
        boolean forDevelopmentOnly() {
            return !setting.profiles().isEmpty() && setting.profiles().stream()
                    .allMatch(profile -> DEVELOPMENT_PROFILES.contains(profile.toLowerCase(Locale.ROOT)));
        }
    }

    static Stream<Found> all(ProjectModel project) {
        return project.settings().stream().flatMap(file -> file.settings().stream().map(setting -> new Found(file, setting)));
    }

    /** What production would run with: every setting not only for development or tests. */
    static Stream<Found> outsideDevelopment(ProjectModel project) {
        return all(project).filter(found -> !found.forDevelopmentOnly());
    }

    /**
     * A Quarkus setting without a profile that a {@code %prod.} one of the same application replaces
     * in production. Quarkus names its production profile; Spring leaves the name to the project, so
     * there is no such rule.
     */
    static boolean replacedInQuarkusProduction(ProjectModel project, Found found) {
        Path application = application(found.file());
        return found.setting().profiles().isEmpty() && all(project).anyMatch(other -> application(other.file()).equals(application)
                && other.setting().profiles().contains(QUARKUS_PRODUCTION) && other.named(found.key()));
    }

    /** The directory of the module whose {@code src/main/resources} holds the file: one application. */
    private static Path application(SettingsFile file) {
        Path path = file.file();
        for (int i = 0; i + 2 < path.getNameCount(); i++) {
            if (path.getName(i).equals(SRC) && path.getName(i + 1).equals(MAIN) && path.getName(i + 2).equals(RESOURCES)) {
                return i == 0 ? Path.of("") : path.subpath(0, i);
            }
        }
        return Optional.ofNullable(path.getParent()).orElse(Path.of(""));
    }

    static String canonical(String key) {
        return key.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
    }
}
