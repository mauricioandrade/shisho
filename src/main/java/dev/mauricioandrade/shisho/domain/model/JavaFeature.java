package dev.mauricioandrade.shisho.domain.model;

/**
 * A Java language or library feature from the catalog: since which version it is final (or the
 * version it is previewed in) and its JEP. {@code markdown} is the didactic explanation.
 */
public record JavaFeature(String id, String name, JavaVersion since, Status status, int jep, String markdown) {

    public enum Status { FINAL, PREVIEW }
}
