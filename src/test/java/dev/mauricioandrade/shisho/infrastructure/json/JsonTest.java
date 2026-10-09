package dev.mauricioandrade.shisho.infrastructure.json;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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

    @Test
    void readsWhatOllamaWrites() {
        assertThat(Json.read("{\"message\":{\"content\":\"a\\u003cb\\u003e \\\"q\\\" \\n 🥋 ção\"},\"done\":false,\"n\":3}"))
                .isEqualTo(Map.of("message", Map.of("content", "a<b> \"q\" \n 🥋 ção"), "done", false, "n", 3));
    }

    @Test
    void readsBackWhatItWrites() {
        Map<String, Object> value = Map.of("text", "aspas \" barra \\ linha\nnova\ttab", "items", List.of(1, true));

        assertThat(Json.read(Json.write(value))).isEqualTo(value);
    }

    @Test
    void rejectsInvalidJson() {
        assertThatIllegalArgumentException().isThrownBy(() -> Json.read("{\"a\": \"sem fim"));
    }
}
