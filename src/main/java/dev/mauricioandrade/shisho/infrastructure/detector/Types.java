package dev.mauricioandrade.shisho.infrastructure.detector;

import dev.mauricioandrade.shisho.domain.model.ClassUnit;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Type names as written in the source, reduced to the simple names they mention. */
final class Types {

    private static final Pattern TYPE_NAME = Pattern.compile("[\\w$.]+");

    private Types() {
    }

    /** {@code Map<String, List<com.shop.Order>>} mentions {@code Map}, {@code String}, {@code List}, {@code Order}. */
    static Set<String> simpleNames(String type) {
        return TYPE_NAME.matcher(type).results()
                .map(match -> match.group().substring(match.group().lastIndexOf('.') + 1))
                .collect(Collectors.toSet());
    }

    /** The types a class receives through its constructors or holds in instance fields. */
    static Set<String> dependencies(ClassUnit unit) {
        Set<String> fromConstructors = unit.constructors().stream()
                .flatMap(constructor -> constructor.parameters().stream())
                .flatMap(parameter -> simpleNames(parameter.type()).stream())
                .collect(Collectors.toSet());
        Set<String> fromFields = unit.fields().stream()
                .filter(field -> !field.isStatic())
                .flatMap(field -> simpleNames(field.type()).stream())
                .collect(Collectors.toSet());
        fromConstructors.addAll(fromFields);
        return fromConstructors;
    }
}
