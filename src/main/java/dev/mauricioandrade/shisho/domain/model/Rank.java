package dev.mauricioandrade.shisho.domain.model;

import java.util.Arrays;

/** The graduation, from 6th kyū (beginner) up to 1st dan, by minimum score. */
public enum Rank {
    DAN_1(92),
    KYU_1(85),
    KYU_2(75),
    KYU_3(65),
    KYU_4(55),
    KYU_5(40),
    KYU_6(0);

    private final int minimumScore;

    Rank(int minimumScore) {
        this.minimumScore = minimumScore;
    }

    public static Rank forScore(int score) {
        return Arrays.stream(values())
                .filter(rank -> score >= rank.minimumScore)
                .findFirst()
                .orElse(KYU_6);
    }
}
