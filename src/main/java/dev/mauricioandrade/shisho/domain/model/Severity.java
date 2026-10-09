package dev.mauricioandrade.shisho.domain.model;

/** How serious a lesson is, and how many points it takes from the score of 100. */
public enum Severity {
    GRAVE(10),
    MEDIA(4),
    LEVE(1),
    INFO(0);

    private final int penalty;

    Severity(int penalty) {
        this.penalty = penalty;
    }

    public int penalty() {
        return penalty;
    }
}
