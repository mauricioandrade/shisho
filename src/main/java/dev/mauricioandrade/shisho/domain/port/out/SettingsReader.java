package dev.mauricioandrade.shisho.domain.port.out;

import dev.mauricioandrade.shisho.domain.model.SettingsFile;

import java.nio.file.Path;
import java.util.List;

public interface SettingsReader {

    /**
     * The application's settings files, in every module, ordered by path; empty when there are
     * none. A file that cannot be read is left out, like a source that does not parse would be
     * reported: settings never stop an analysis.
     */
    List<SettingsFile> read(Path projectRoot);
}
