package dev.mauricioandrade.shisho.infrastructure.scaffold;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The catalog's template syntax, and nothing more: {@code {{name}}} is a value; {@code {{#list}}…{{/list}}}
 * repeats its text for each item of a list, where {@code {{,}}} is {@code ", "} except after the
 * last item. A line holding only a section tag disappears, trailing spaces are removed and blank
 * lines never repeat. See catalog/README.md.
 */
final class Template {

    private static final Pattern STANDALONE_TAG = Pattern.compile("(?m)^[ \\t]*(\\{\\{[#/]\\w+}})[ \\t]*\\n");
    private static final Pattern SECTION = Pattern.compile("(?s)\\{\\{#(\\w+)}}(.*?)\\{\\{/\\1}}");
    private static final Pattern VALUE = Pattern.compile("\\{\\{(\\w+)}}");
    private static final String SEPARATOR = "{{,}}";
    private static final Pattern TRAILING_SPACES = Pattern.compile("(?m)[ \\t]+$");
    private static final Pattern REPEATED_BLANK_LINES = Pattern.compile("\\n{3,}");

    private Template() {
    }

    /**
     * @throws IllegalArgumentException if the template names a value or a list it was not given:
     *         a broken catalog entry
     */
    static String render(String template, Map<String, String> values, Map<String, List<Map<String, String>>> lists) {
        String withoutTagLines = STANDALONE_TAG.matcher(template).replaceAll("$1");
        String expanded = SECTION.matcher(withoutTagLines).replaceAll(section -> Matcher.quoteReplacement(
                repeat(section.group(1), section.group(2), values, lists)));
        String filled = values(expanded, values);
        return REPEATED_BLANK_LINES.matcher(TRAILING_SPACES.matcher(filled).replaceAll("")).replaceAll("\n\n");
    }

    private static String repeat(String list, String text, Map<String, String> values, Map<String, List<Map<String, String>>> lists) {
        List<Map<String, String>> items = lists.get(list);
        if (items == null) {
            throw new IllegalArgumentException("Unknown list in template: " + list);
        }
        return IntStream.range(0, items.size())
                .mapToObj(i -> {
                    Map<String, String> scope = new HashMap<>(values);
                    scope.putAll(items.get(i));
                    return values(text.replace(SEPARATOR, i < items.size() - 1 ? ", " : ""), scope);
                })
                .collect(Collectors.joining());
    }

    private static String values(String text, Map<String, String> values) {
        return VALUE.matcher(text).replaceAll(match -> {
            String value = values.get(match.group(1));
            if (value == null) {
                throw new IllegalArgumentException("Unknown value in template: " + match.group(1));
            }
            return Matcher.quoteReplacement(value);
        });
    }
}
