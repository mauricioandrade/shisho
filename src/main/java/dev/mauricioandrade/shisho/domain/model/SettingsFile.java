package dev.mauricioandrade.shisho.domain.model;

import java.nio.file.Path;
import java.util.List;

/**
 * A file of the analyzed application's own settings, read as flat keys ({@code server.port}): the
 * framework's configuration, not the Shishō's ({@link ProjectConfig}). {@code file} is relative to
 * the project root.
 */
public record SettingsFile(Path file, List<Setting> settings) {

    public SettingsFile {
        settings = List.copyOf(settings);
    }

    /**
     * @param profiles the profiles the setting applies to, from the file's name or a prefix of the
     *                 key; empty when it applies to every profile
     */
    public record Setting(String key, String value, int line, List<String> profiles) {

        public Setting {
            profiles = List.copyOf(profiles);
        }
    }
}
