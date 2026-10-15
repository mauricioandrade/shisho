package dev.mauricioandrade.shisho.domain.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What the catalog teaches about a kata, for the sensei mode: why it exists, when breaking it is
 * fine, the code before and after the fix, and the sources behind it.
 *
 * @param why            paragraphs explaining the trade-off
 * @param acceptableWhen paragraphs on when the flagged code is a reasonable choice
 * @param variants       the before and after written for a framework other than the default, by
 *                       the variant of the lesson's message key ({@code kata.<id>.lesson.<variant>})
 * @param references     URLs of primary sources
 */
public record KataGuide(String kataId, List<String> why, List<String> acceptableWhen, Snippet before, Snippet after,
                        Map<String, Example> variants, List<String> references) {

    public KataGuide {
        why = List.copyOf(why);
        acceptableWhen = List.copyOf(acceptableWhen);
        variants = Map.copyOf(variants);
        references = List.copyOf(references);
    }

    public KataGuide(String kataId, List<String> why, List<String> acceptableWhen, Snippet before, Snippet after,
                     List<String> references) {
        this(kataId, why, acceptableWhen, before, after, Map.of(), references);
    }

    /** This guide with the before and after of {@code variant}, or unchanged if it has none. */
    public KataGuide in(Optional<String> variant) {
        return variant.map(variants::get)
                .map(example -> new KataGuide(kataId, why, acceptableWhen, example.before(), example.after(), variants, references))
                .orElse(this);
    }

    /** A code block in {@code language} ({@code java}, {@code xml}...) and the prose around it, possibly empty. */
    public record Snippet(String language, String code, String explanation) {
    }

    /** The code before and after the fix. */
    public record Example(Snippet before, Snippet after) {
    }
}
