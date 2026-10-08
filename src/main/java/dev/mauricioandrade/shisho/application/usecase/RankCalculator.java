package dev.mauricioandrade.shisho.application.usecase;

import dev.mauricioandrade.shisho.domain.model.Lesson;

import java.util.List;

/** Score starts at 100 and each lesson costs points by severity; it never goes below 0. */
public final class RankCalculator {

    private static final int FULL_SCORE = 100;

    public int score(List<Lesson> lessons) {
        int penalty = lessons.stream().mapToInt(lesson -> switch (lesson.severity()) {
            case GRAVE -> 10;
            case MEDIA -> 4;
            case LEVE -> 1;
            case INFO -> 0;
        }).sum();
        return Math.max(0, FULL_SCORE - penalty);
    }
}
