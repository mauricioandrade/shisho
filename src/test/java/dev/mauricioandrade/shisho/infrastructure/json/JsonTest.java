package dev.mauricioandrade.shisho.infrastructure.json;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonTest {

    @Test
    void writesNestedValuesIndented() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", "Shishō");
        value.put("count", 2);
        value.put("ok", true);
        value.put("none", null);
        value.put("items", List.of(1, Map.of("a", "b")));
        value.put("empty", List.of());
        value.put("nothing", Map.of());

        assertThat(Json.write(value)).isEqualTo("""
                {
                  "name": "Shishō",
                  "count": 2,
                  "ok": true,
                  "none": null,
                  "items": [
                    1,
                    {
                      "a": "b"
                    }
                  ],
                  "empty": [],
                  "nothing": {}
                }
                """);
    }

    @Test
    void escapesStrings() {
        assertThat(Json.write(Arrays.asList("aspas \" barra \\ linha\nnova\ttab\u0001")))
                .isEqualTo("[\n  \"aspas \\\" barra \\\\ linha\\nnova\\ttab\\u0001\"\n]\n");
    }
}
