package dev.mauricioandrade.shisho.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A package pattern from the catalog, such as {@code domain/model} or {@code <feature>/controller}:
 * segments separated by {@code /} that must appear consecutively in a package name, where a
 * {@code <placeholder>} stands for any one segment.
 */
public record PackagePattern(List<String> segments) {

    public PackagePattern {
        segments = List.copyOf(segments);
    }

    public static PackagePattern parse(String pattern) {
        return new PackagePattern(List.of(pattern.split("/")));
    }

    public boolean hasPlaceholder() {
        return segments.stream().anyMatch(PackagePattern::isPlaceholder);
    }

    public boolean matches(String packageName) {
        return bindings(packageName).isPresent();
    }

    /** The segments bound to the placeholders where the pattern first occurs, or empty if it does not occur. */
    public Optional<List<String>> bindings(String packageName) {
        List<String> names = List.of(packageName.split("\\."));
        for (int start = 0; start + segments.size() <= names.size(); start++) {
            Optional<List<String>> bound = bindingsAt(names, start);
            if (bound.isPresent()) {
                return bound;
            }
        }
        return Optional.empty();
    }

    private Optional<List<String>> bindingsAt(List<String> names, int start) {
        List<String> bound = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            String expected = segments.get(i);
            String actual = names.get(start + i);
            if (isPlaceholder(expected)) {
                bound.add(actual);
            } else if (!expected.equals(actual)) {
                return Optional.empty();
            }
        }
        return Optional.of(bound);
    }

    private static boolean isPlaceholder(String segment) {
        return segment.startsWith("<") && segment.endsWith(">");
    }
}
