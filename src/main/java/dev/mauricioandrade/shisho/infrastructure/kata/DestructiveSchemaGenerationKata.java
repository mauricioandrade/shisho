package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.infrastructure.kata.ApplicationSettings.Found;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Hibernate told to drop the schema when the application starts ({@code ddl-auto=create-drop},
 * Quarkus' {@code drop-and-create}) in a setting production also reads. In Quarkus, a setting
 * without a profile prefix is production's too, unless a {@code %prod.} one replaces it.
 */
final class DestructiveSchemaGenerationKata implements ProjectKata {

    private static final Set<String> HIBERNATE_DROPS = Set.of("create", "create-drop");
    private static final Set<String> JPA_DROPS = Set.of("drop-and-create", "drop");
    private static final Map<String, Set<String>> SPRING = Map.of(
            "spring.jpa.hibernate.ddl-auto", HIBERNATE_DROPS,
            "spring.jpa.properties.hibernate.hbm2ddl.auto", HIBERNATE_DROPS,
            "spring.jpa.properties.jakarta.persistence.schema-generation.database.action", JPA_DROPS,
            "spring.jpa.properties.javax.persistence.schema-generation.database.action", JPA_DROPS);
    /** Also a named persistence unit: {@code quarkus.hibernate-orm."orders".database.generation}. */
    private static final Pattern QUARKUS = Pattern.compile("quarkus\\.hibernate-orm(\\..+)?\\.(schema-management\\.strategy|database\\.generation)");

    @Override
    public String id() {
        return "destructive-schema-generation";
    }

    @Override
    public Severity severity() {
        return Severity.GRAVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        return ApplicationSettings.outsideDevelopment(project)
                .<Lesson>mapMulti((found, lessons) -> {
                    if (QUARKUS.matcher(found.key()).matches() && JPA_DROPS.contains(found.value())
                            && !ApplicationSettings.replacedInQuarkusProduction(project, found)) {
                        lessons.accept(QuarkusApplications.lesson(this, found.file().file(), found.setting().line(),
                                found.key(), found.setting().value().trim()));
                    } else if (dropsInSpring(found)) {
                        lessons.accept(lesson(found.file().file(), found.setting().line(), found.key(), found.setting().value().trim()));
                    }
                })
                .toList();
    }

    private static boolean dropsInSpring(Found found) {
        return SPRING.entrySet().stream().anyMatch(entry -> found.named(entry.getKey()) && entry.getValue().contains(found.value()));
    }
}
