package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.List;

/** Score starts at {@value #FULL_SCORE} and each lesson costs the penalty of its severity; it never goes below 0. */
public final class RankCalculator {

    private static final int FULL_SCORE = 100;

    public int score(List<Lesson> lessons) {
        int penalty = lessons.stream().mapToInt(lesson -> lesson.severity().penalty()).sum();
        return Math.max(0, FULL_SCORE - penalty);
    }
}
