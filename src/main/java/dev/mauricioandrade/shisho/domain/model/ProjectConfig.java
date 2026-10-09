package dev.mauricioandrade.shisho.domain.model;

import java.util.Map;
import java.util.Optional;

/**
 * The analyzed project's own settings: katas turned off and severities overridden, by kata id.
 * Dependency rule violations are configured like a kata, under {@link ArchitectureMatch#DEPENDENCY_RULE}.
 */
public record ProjectConfig(Map<String, KataSetting> katas) {

    public static final ProjectConfig DEFAULT = new ProjectConfig(Map.of());

    public ProjectConfig {
        katas = Map.copyOf(katas);
    }

    public boolean isEnabled(String kataId) {
        return setting(kataId).map(KataSetting::enabled).orElse(true);
    }

    public Severity severity(String kataId, Severity standard) {
        return setting(kataId).flatMap(KataSetting::severity).orElse(standard);
    }

    private Optional<KataSetting> setting(String kataId) {
        return Optional.ofNullable(katas.get(kataId));
    }

    public record KataSetting(boolean enabled, Optional<Severity> severity) {
    }
}
