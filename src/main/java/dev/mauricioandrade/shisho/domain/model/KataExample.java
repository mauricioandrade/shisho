package dev.mauricioandrade.shisho.domain.model;

/** How a lesson is fixed, from the catalog: the code before and after, for the sensei mode. */
public record KataExample(String kataId, Snippet before, Snippet after) {

    /** A code block in {@code language} ({@code java}, {@code xml}...) and the prose around it, possibly empty. */
    public record Snippet(String language, String code, String explanation) {
    }
}
