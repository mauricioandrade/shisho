package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class QuarkusPrivateInjectionKataTest {

    private static final String SERVICE = """
            @ApplicationScoped
            class OrderService {
                @Inject
                private OrderRepository orders;
                @ConfigProperty(name = "orders.limit")
                private int limit;
                @Inject
                Clock clock;
                private final Mailer mailer;

                OrderService(Mailer mailer) {
                    this.mailer = mailer;
                }
            }
            """;

    private final QuarkusPrivateInjectionKata kata = new QuarkusPrivateInjectionKata();

    @Test
    void flagsPrivateInjectedFieldsInQuarkusApplications() {
        assertThat(evaluate(kata, SERVICE, "io.quarkus:quarkus-arc")).satisfiesExactly(
                first -> {
                    assertThat(first.line()).isEqualTo(3);
                    assertThat(first.arguments()).containsExactly("orders");
                    assertThat(first.messageKey()).isEqualTo("kata.quarkus-private-injection.lesson");
                },
                second -> assertThat(second.arguments()).containsExactly("limit"));
    }

    @Test
    void leavesOtherProjectsToFieldInjection() {
        assertThat(evaluate(kata, SERVICE, "org.springframework.boot:spring-boot-starter-web")).isEmpty();
        assertThat(evaluate(kata, SERVICE)).isEmpty();
    }
}
