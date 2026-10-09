package dev.mauricioandrade.shisho.domain.model;

import java.util.List;

/**
 * What the catalog teaches about a kata, for the sensei mode: why it exists, when breaking it is
 * fine, the code before and after the fix, and the sources behind it.
 *
 * @param why            paragraphs explaining the trade-off
 * @param acceptableWhen paragraphs on when the flagged code is a reasonable choice
 * @param references     URLs of primary sources
 */
public record KataGuide(String kataId, List<String> why, List<String> acceptableWhen, Snippet before, Snippet after,
                        List<String> references) {

    public KataGuide {
        why = List.copyOf(why);
        acceptableWhen = List.copyOf(acceptableWhen);
        references = List.copyOf(references);
    }

    /** A code block in {@code language} ({@code java}, {@code xml}...) and the prose around it, possibly empty. */
    public record Snippet(String language, String code, String explanation) {
    }
}
