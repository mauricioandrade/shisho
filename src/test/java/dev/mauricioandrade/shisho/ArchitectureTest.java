package dev.mauricioandrade.shisho;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.SourceFile;
import dev.mauricioandrade.shisho.infrastructure.parser.JavaParserSourceParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The dependency rules of CLAUDE.md §5, checked with the Shishō's own parser. */
class ArchitectureTest {

    private static final String ROOT = "dev.mauricioandrade.shisho.";

    private final List<ClassUnit> classes = new JavaParserSourceParser().parse(Path.of(""), new JavaVersion(25)).stream()
            .map(SourceFile.Parsed.class::cast)
            .flatMap(parsed -> parsed.classes().stream())
            .toList();

    @Test
    void domainDependsOnNothingButTheJdk() {
        assertThat(importsOf(ROOT + "domain")).noneMatch(name ->
                name.startsWith(ROOT + "application")
                        || name.startsWith(ROOT + "infrastructure")
                        || name.startsWith("picocli")
                        || name.startsWith("org.yaml")
                        || name.startsWith("com.github.javaparser"));
    }

    @Test
    void applicationDoesNotDependOnInfrastructure() {
        assertThat(importsOf(ROOT + "application")).isNotEmpty()
                .noneMatch(name -> name.startsWith(ROOT + "infrastructure"));
    }

    private List<String> importsOf(String packagePrefix) {
        return classes.stream()
                .filter(unit -> unit.packageName().startsWith(packagePrefix))
                .flatMap(unit -> unit.imports().stream())
                .map(ClassUnit.Import::name)
                .toList();
    }
}
