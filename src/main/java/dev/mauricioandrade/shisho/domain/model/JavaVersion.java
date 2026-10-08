package dev.mauricioandrade.shisho.domain.model;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A Java feature release, such as 21 or 25. Legacy {@code 1.x} notation maps to {@code x}. */
public record JavaVersion(int feature) implements Comparable<JavaVersion> {

    private static final Pattern NOTATION = Pattern.compile("(?:1\\.)?([1-9]\\d*)");

    public JavaVersion {
        if (feature < 1) {
            throw new IllegalArgumentException("Java feature release must be positive: " + feature);
        }
    }

    /** Parses {@code "21"}, {@code "1.8"} and the like; anything else is empty. */
    public static Optional<JavaVersion> parse(String text) {
        Matcher matcher = NOTATION.matcher(text.strip());
        return matcher.matches()
                ? Optional.of(new JavaVersion(Integer.parseInt(matcher.group(1))))
                : Optional.empty();
    }

    public boolean isAtLeast(JavaVersion other) {
        return compareTo(other) >= 0;
    }

    @Override
    public int compareTo(JavaVersion other) {
        return Integer.compare(feature, other.feature);
    }

    @Override
    public String toString() {
        return Integer.toString(feature);
    }
}
