package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.Lesson;
import dev.mauricioandrade.shisho.domain.model.ProjectKata;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Severity;
import dev.mauricioandrade.shisho.infrastructure.kata.ApplicationSettings.Found;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Every SQL statement written to the log ({@code spring.jpa.show-sql}, Quarkus'
 * {@code log.sql}, the {@code org.hibernate.SQL} logger at debug) in a setting production reads.
 */
final class SqlLoggingKata implements ProjectKata {

    private static final Set<String> SPRING_SWITCHES = Set.of("spring.jpa.show-sql", "spring.jpa.properties.hibernate.show_sql");
    private static final String SPRING_LOGGER = "logging.level.org.hibernate.SQL";
    private static final Set<String> VERBOSE_LEVELS = Set.of("debug", "trace", "all");
    /** Also a named persistence unit: {@code quarkus.hibernate-orm."orders".log.sql}. */
    private static final Pattern QUARKUS = Pattern.compile("quarkus\\.hibernate-orm(\\..+)?\\.log\\.sql");

    @Override
    public String id() {
        return "sql-logging";
    }

    @Override
    public Severity severity() {
        return Severity.LEVE;
    }

    @Override
    public JavaVersion minJavaVersion() {
        return JavaVersion.MINIMUM;
    }

    @Override
    public List<Lesson> evaluate(ProjectModel project) {
        return ApplicationSettings.outsideDevelopment(project)
                .<Lesson>mapMulti((found, lessons) -> {
                    if (QUARKUS.matcher(found.key()).matches() && found.value().equals("true")
                            && !ApplicationSettings.replacedInQuarkusProduction(project, found)) {
                        lessons.accept(QuarkusApplications.lesson(this, found.file().file(), found.setting().line(), found.key()));
                    } else if (logsInSpring(found)) {
                        lessons.accept(lesson(found.file().file(), found.setting().line(), found.key()));
                    }
                })
                .toList();
    }

    private static boolean logsInSpring(Found found) {
        return SPRING_SWITCHES.stream().anyMatch(found::named) && found.value().equals("true")
                || found.named(SPRING_LOGGER) && VERBOSE_LEVELS.contains(found.value());
    }
}
