package dev.mauricioandrade.shisho.domain.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class RankTest {

    @ParameterizedTest
    @CsvSource({
            "100, DAN_1", "92, DAN_1",
            "91, KYU_1", "85, KYU_1",
            "84, KYU_2", "75, KYU_2",
            "74, KYU_3", "65, KYU_3",
            "64, KYU_4", "55, KYU_4",
            "54, KYU_5", "40, KYU_5",
            "39, KYU_6", "0, KYU_6"})
    void bandsFollowTheScore(int score, Rank rank) {
        assertThat(Rank.forScore(score)).isEqualTo(rank);
    }
}
