package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class RequestBodyWithoutValidKataTest {

    private final RequestBodyWithoutValidKata kata = new RequestBodyWithoutValidKata();

    @Test
    void flagsRequestBodyWithoutValidation() {
        assertThat(evaluate(kata, """
                @RestController
                class OrderController {
                    @PostMapping
                    void create(
                            @RequestBody CreateOrderRequest request) {}
                }
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(5);
            assertThat(lesson.arguments()).containsExactly("CreateOrderRequest");
        });
    }

    @Test
    void acceptsValidOrValidated() {
        assertThat(evaluate(kata, """
                @RestController
                class OrderController {
                    void create(@Valid @RequestBody CreateOrderRequest request) {}
                    void update(@RequestBody @Validated UpdateOrderRequest request) {}
                }
                """)).isEmpty();
    }

    @Test
    void ignoresBodiesWithNothingToValidate() {
        assertThat(evaluate(kata, """
                @RestController
                class WebhookController {
                    void raw(@RequestBody String payload) {}
                    void bytes(@RequestBody byte[] payload) {}
                    void map(@RequestBody Map<String, Object> payload) {}
                }
                """)).isEmpty();
    }
}
