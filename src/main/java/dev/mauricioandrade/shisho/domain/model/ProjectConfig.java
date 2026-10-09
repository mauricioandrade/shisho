package dev.mauricioandrade.shisho.domain.model;

import java.util.Map;
import java.util.Optional;

/** The analyzed project's own settings: katas turned off and severities overridden, by kata id. */
public record ProjectConfig(Map<String, KataSetting> katas) {

    public static final ProjectConfig DEFAULT = new ProjectConfig(Map.of());

    public ProjectConfig {
        katas = Map.copyOf(katas);
    }

    public boolean isEnabled(Kata kata) {
        return setting(kata).map(KataSetting::enabled).orElse(true);
    }

    public Severity severity(Kata kata) {
        return setting(kata).flatMap(KataSetting::severity).orElse(kata.severity());
    }

    private Optional<KataSetting> setting(Kata kata) {
        return Optional.ofNullable(katas.get(kata.id()));
    }

    public record KataSetting(boolean enabled, Optional<Severity> severity) {
    }
}
