package dev.mauricioandrade.shisho.infrastructure.build;

import dev.mauricioandrade.shisho.domain.model.BuildInfo;
import dev.mauricioandrade.shisho.domain.port.out.BuildFileReader;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** The build of whichever tool the project uses: Maven when there is a {@code pom.xml}, else Gradle. */
public final class BuildFileReaders implements BuildFileReader {

    private final List<BuildFileReader> readers;

    public BuildFileReaders(List<BuildFileReader> readers) {
        this.readers = List.copyOf(readers);
    }

    public static BuildFileReaders all() {
        return new BuildFileReaders(List.of(new MavenBuildFileReader(), new GradleBuildFileReader()));
    }

    @Override
    public Optional<BuildInfo> read(Path projectRoot) {
        return readers.stream()
                .map(reader -> reader.read(projectRoot))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
