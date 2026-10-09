package dev.mauricioandrade.shisho.infrastructure.json;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Writes maps, lists, strings, numbers and booleans as indented JSON: enough for SARIF and for
 * requests to the local LLM, so a library would add more than it saves.
 */
public final class Json {

    private static final String INDENT = "  ";

    private Json() {
    }

    public static String write(Object value) {
        StringBuilder json = new StringBuilder();
        write(value, json, "");
        return json.append('\n').toString();
    }

    private static void write(Object value, StringBuilder json, String indent) {
        switch (value) {
            case null -> json.append("null");
            case String text -> string(text, json);
            case Number number -> json.append(number);
            case Boolean flag -> json.append(flag);
            case Map<?, ?> map -> object(map, json, indent);
            case List<?> list -> array(list, json, indent);
            default -> throw new IllegalArgumentException("Not a JSON value: " + value.getClass());
        }
    }

    private static void object(Map<?, ?> map, StringBuilder json, String indent) {
        if (map.isEmpty()) {
            json.append("{}");
            return;
        }
        json.append("{\n");
        Iterator<? extends Map.Entry<?, ?>> entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<?, ?> entry = entries.next();
            json.append(indent).append(INDENT);
            string(String.valueOf(entry.getKey()), json);
            json.append(": ");
            write(entry.getValue(), json, indent + INDENT);
            json.append(entries.hasNext() ? ",\n" : "\n");
        }
        json.append(indent).append('}');
    }

    private static void array(List<?> list, StringBuilder json, String indent) {
        if (list.isEmpty()) {
            json.append("[]");
            return;
        }
        json.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            json.append(indent).append(INDENT);
            write(list.get(i), json, indent + INDENT);
            json.append(i < list.size() - 1 ? ",\n" : "\n");
        }
        json.append(indent).append(']');
    }

    private static void string(String text, StringBuilder json) {
        json.append('"');
        for (char character : text.toCharArray()) {
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        json.append('"');
    }
}
