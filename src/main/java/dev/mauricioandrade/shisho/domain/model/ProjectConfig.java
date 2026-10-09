package dev.mauricioandrade.shisho.domain.model;

import java.util.Map;
import java.util.Optional;

/**
 * The analyzed project's own settings: katas turned off and severities overridden, by kata id,
 * and the declared context, empty when the author did not declare one. Dependency rule
 * violations are configured like a kata, under {@link ArchitectureMatch#DEPENDENCY_RULE}.
 */
public record ProjectConfig(Map<String, KataSetting> katas, Optional<ProjectContext> context) {

    public static final ProjectConfig DEFAULT = new ProjectConfig(Map.of());

    public ProjectConfig {
        katas = Map.copyOf(katas);
    }

    public ProjectConfig(Map<String, KataSetting> katas) {
        this(katas, Optional.empty());
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
