package dev.mauricioandrade.shisho.domain.model;

/** What the catalog teaches about a kata, for the sensei mode: the code before and after the fix. */
public record KataGuide(String kataId, Snippet before, Snippet after) {

    /** A code block in {@code language} ({@code java}, {@code xml}...) and the prose around it, possibly empty. */
    public record Snippet(String language, String code, String explanation) {
    }
}
