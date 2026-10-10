package dev.mauricioandrade.shisho.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A package pattern from the catalog, such as {@code domain/model} or {@code <feature>/controller}:
 * segments separated by {@code /} that must appear consecutively in a package name, where a
 * {@code <placeholder>} stands for any one segment. A pattern ending in a class segment, such as
 * {@code <feature>/*Controller}, is matched against qualified class names instead and must end at
 * the class: {@code *Controller} stands for a class name ending in {@code Controller}. A segment
 * may list alternatives, such as {@code domain|dominio} or {@code *Controller|*Controlador}, so
 * that one pattern covers code written in English and in Portuguese.
 */
public record PackagePattern(List<String> segments) {

    public PackagePattern {
        segments = List.copyOf(segments);
    }

    public static PackagePattern parse(String pattern) {
        return new PackagePattern(List.of(pattern.split("/")));
    }

    /** The pattern as a report shows it: the first alternative of each segment, such as {@code domain/model}. */
    public String display() {
        return segments.stream().map(segment -> alternatives(segment).getFirst()).collect(Collectors.joining("/"));
    }

    /** Whether the pattern ends in a class segment ({@code *Controller}), matched against qualified class names. */
    public boolean isClassPattern() {
        return isClassSegment(segments.getLast());
    }

    public boolean hasPlaceholder() {
        return segments.stream().anyMatch(PackagePattern::isPlaceholder);
    }

    public boolean matches(String packageName) {
        return bindings(packageName).isPresent();
    }

    /**
     * The segments bound to the placeholders where the pattern first occurs, or empty if it does
     * not occur. {@code name} is a package name, or a qualified class name for a class pattern.
     */
    public Optional<List<String>> bindings(String name) {
        List<String> names = List.of(name.split("\\."));
        int first = isClassPattern() ? Math.max(0, names.size() - segments.size()) : 0;
        for (int start = first; start + segments.size() <= names.size(); start++) {
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
            } else if (isClassSegment(expected)) {
                boolean suffixed = alternatives(expected).stream().map(alternative -> alternative.substring(1))
                        .anyMatch(suffix -> actual.endsWith(suffix) && actual.length() > suffix.length());
                if (!suffixed) {
                    return Optional.empty();
                }
            } else if (!alternatives(expected).contains(actual)) {
                return Optional.empty();
            }
        }
        return Optional.of(bound);
    }

    private static List<String> alternatives(String segment) {
        return List.of(segment.split("\\|"));
    }

    private static boolean isClassSegment(String segment) {
        return segment.startsWith("*");
    }

    private static boolean isPlaceholder(String segment) {
        return segment.startsWith("<") && segment.endsWith(">");
    }
}
