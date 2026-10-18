package dev.mauricioandrade.shisho.domain.port.in;

import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.nio.file.Path;
import java.util.List;

public interface RecordBaseline {

    /**
     * Analyzes the project and records every lesson it has now, the known ones included, as its
     * baseline.
     *
     * @throws dev.mauricioandrade.shisho.domain.exception.ShishoException if the project cannot
     *         be analyzed or the baseline cannot be written
     */
    Recorded record(Path projectRoot);

    /** @param file the baseline written, relative to the project root */
    record Recorded(Path file, List<Lesson> lessons) {

        public Recorded {
            lessons = List.copyOf(lessons);
        }
    }
}
