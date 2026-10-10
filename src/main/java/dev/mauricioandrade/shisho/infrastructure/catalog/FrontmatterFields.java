package dev.mauricioandrade.shisho.infrastructure.catalog;

import dev.mauricioandrade.shisho.domain.exception.CatalogException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Typed, validating view over a parsed YAML frontmatter. Every error names the file and the
 * full field path (e.g. {@code assinatura.minimo_sinais}).
 */
final class FrontmatterFields {

    private final String fileName;
    private final String path;
    private final Map<?, ?> values;

    FrontmatterFields(String fileName, Map<?, ?> values) {
        this(fileName, "", values);
    }

    private FrontmatterFields(String fileName, String path, Map<?, ?> values) {
        this.fileName = fileName;
        this.path = path;
        this.values = values;
    }

    /**
     * These fields with the translation's on top. A translation carries only text, so any other
     * key in it is an error: the data stays in one place, the original.
     */
    FrontmatterFields translatedBy(FrontmatterFields translation, Set<String> translatable) {
        Map<Object, Object> merged = new LinkedHashMap<>(values);
        for (Map.Entry<?, ?> entry : translation.values.entrySet()) {
            if (!translatable.contains(String.valueOf(entry.getKey()))) {
                throw translation.invalid(String.valueOf(entry.getKey()), entry.getValue());
            }
            merged.put(entry.getKey(), entry.getValue());
        }
        return new FrontmatterFields(fileName, path, merged);
    }

    String text(String key) {
        return switch (required(key)) {
            case String text when !text.isBlank() -> text;
            case Object other -> throw invalid(key, other);
        };
    }

    Optional<String> optionalText(String key) {
        return values.get(key) == null ? Optional.empty() : Optional.of(text(key));
    }

    int integer(String key) {
        return switch (required(key)) {
            case Integer number -> number;
            case Object other -> throw invalid(key, other);
        };
    }

    List<String> textList(String key) {
        if (!(required(key) instanceof List<?> items)) {
            throw invalid(key, values.get(key));
        }
        List<String> texts = new ArrayList<>(items.size());
        for (Object item : items) {
            switch (item) {
                case String text when !text.isBlank() -> texts.add(text);
                case null, default -> throw invalid(key, item);
            }
        }
        return List.copyOf(texts);
    }

    List<String> optionalTextList(String key) {
        return values.containsKey(key) ? textList(key) : List.of();
    }

    /** A map of texts to texts, such as {@code uma_vez_por_projeto}; empty when the key is absent. */
    Map<String, String> optionalTextMap(String key) {
        if (!values.containsKey(key)) {
            return Map.of();
        }
        if (!(values.get(key) instanceof Map<?, ?> map)) {
            throw invalid(key, values.get(key));
        }
        Map<String, String> texts = new LinkedHashMap<>();
        map.forEach((name, value) -> {
            if (!(name instanceof String text && !text.isBlank() && value instanceof String other && !other.isBlank())) {
                throw invalid(key, name + ": " + value);
            }
            texts.put(text, other);
        });
        return texts;
    }

    FrontmatterFields section(String key) {
        return switch (required(key)) {
            case Map<?, ?> map -> new FrontmatterFields(fileName, qualified(key) + ".", map);
            case Object other -> throw invalid(key, other);
        };
    }

    List<FrontmatterFields> sectionList(String key) {
        if (!(required(key) instanceof List<?> items)) {
            throw invalid(key, values.get(key));
        }
        List<FrontmatterFields> sections = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            String itemPath = qualified(key) + "[" + i + "].";
            switch (items.get(i)) {
                case Map<?, ?> map -> sections.add(new FrontmatterFields(fileName, itemPath, map));
                case null, default -> throw invalid(key, items.get(i));
            }
        }
        return List.copyOf(sections);
    }

    /** The keys present, in file order. */
    List<String> keys() {
        return values.keySet().stream().map(String::valueOf).toList();
    }

    CatalogException invalid(String key, Object value) {
        return new CatalogException("catalog.error.invalid-value", fileName, qualified(key), String.valueOf(value));
    }

    private Object required(String key) {
        Object value = values.get(key);
        if (value == null) {
            throw new CatalogException("catalog.error.missing-field", fileName, qualified(key));
        }
        return value;
    }

    private String qualified(String key) {
        return path + key;
    }
}
