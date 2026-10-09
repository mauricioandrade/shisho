package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.ClassRange;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Complexity;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Cost;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Family;
import dev.mauricioandrade.shisho.domain.model.ArchitectureProfile.Signature;
import dev.mauricioandrade.shisho.domain.model.Detector;
import dev.mauricioandrade.shisho.domain.model.Evidence;
import dev.mauricioandrade.shisho.domain.model.JavaVersion;
import dev.mauricioandrade.shisho.domain.model.ProjectContext;
import dev.mauricioandrade.shisho.domain.model.ProjectModel;
import dev.mauricioandrade.shisho.domain.model.Verdict;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureJudgeTest {

    private static final ProjectModel PROJECT = new ProjectModel(new JavaVersion(21), Optional.empty(), List.of());
    private static final ArchitectureProfile CLEAN = architecture(Complexity.HIGH,
            List.of("use_cases_que_so_delegam", "dominio_anemico"), List.of(), Optional.of("package-by-feature"), List.of());
    private static final ArchitectureProfile LAYERED = architecture(Complexity.LOW,
            List.of(), List.of("service_deus"), Optional.empty(), List.of("package-by-feature"));

    @Test
    void measuredUnderengineeringComesFirst() {
        ArchitectureJudge judge = new ArchitectureJudge(List.of(firing("service_deus"), firing("dominio_anemico")));

        assertThat(judge.judge(LAYERED, PROJECT, Optional.empty())).isEqualTo(new Verdict.Underengineering(
                List.of(new Evidence("service_deus", List.of())), List.of("package-by-feature")));
    }

    @Test
    void measuredOverengineeringSkipsSignalsWithoutDetector() {
        ArchitectureJudge judge = new ArchitectureJudge(List.of(firing("dominio_anemico")));

        assertThat(judge.judge(CLEAN, PROJECT, context("vida_util_longa"))).isEqualTo(new Verdict.Overengineering(
                List.of(new Evidence("dominio_anemico", List.of())), false, Optional.of("package-by-feature")));
    }

    @Test
    void aDeclaredContextThatPaysForNothingIsOverengineering() {
        ArchitectureJudge judge = new ArchitectureJudge(List.of(silent("dominio_anemico")));

        assertThat(judge.judge(CLEAN, PROJECT, context("mvp"))).isEqualTo(
                new Verdict.Overengineering(List.of(), true, Optional.of("package-by-feature")));
    }

    @Test
    void aJustifiedOrSimpleArchitectureIsAdequate() {
        ArchitectureJudge judge = new ArchitectureJudge(List.of(silent("dominio_anemico"), silent("service_deus")));

        assertThat(judge.judge(CLEAN, PROJECT, context("mvp", "vida_util_longa")))
                .isEqualTo(new Verdict.Adequate(true, List.of("vida_util_longa")));
        assertThat(judge.judge(LAYERED, PROJECT, context("mvp")))
                .isEqualTo(new Verdict.Adequate(true, List.of()));
        assertThat(judge.judge(CLEAN, PROJECT, Optional.empty()))
                .isEqualTo(new Verdict.Adequate(false, List.of()));
    }

    private static Optional<ProjectContext> context(String... keys) {
        return Optional.of(new ProjectContext(Set.of(keys)));
    }

    private static Detector firing(String id) {
        return detector(id, true);
    }

    private static Detector silent(String id) {
        return detector(id, false);
    }

    private static Detector detector(String id, boolean fires) {
        return new Detector() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public Optional<Evidence> detect(ProjectModel project) {
                return fires ? evidence() : Optional.empty();
            }
        };
    }

    private static ArchitectureProfile architecture(Complexity complexity, List<String> over, List<String> under,
                                                    Optional<String> simpler, List<String> evolvesTo) {
        return new ArchitectureProfile("arch", "Arch", Family.SIMPLE, new Signature(List.of(), List.of(), 0), List.of(),
                new Cost(new ClassRange(4, 6), complexity), List.of("vida_util_longa", "time_grande"), over, under,
                evolvesTo, simpler, List.of(), "");
    }
}
