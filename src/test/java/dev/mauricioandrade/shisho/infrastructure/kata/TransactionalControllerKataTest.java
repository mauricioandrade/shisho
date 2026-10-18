package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionalControllerKataTest {

    private final TransactionalControllerKata kata = new TransactionalControllerKata();

    @Test
    void flagsTransactionsOnControllerClassesAndMethods() {
        assertThat(evaluate(kata, """
                @RestController
                @Transactional
                class AuthorityResource {}

                @Controller
                class PageController {
                    @Transactional(readOnly = true)
                    String list() { return "list"; }
                }
                """)).extracting(Lesson::line).containsExactly(2, 7);
    }

    @Test
    void acceptsServicesAndJakartaRestResources() {
        assertThat(evaluate(kata, """
                @Service
                @Transactional
                class AuthorityService {}

                @Path("/fruits")
                class FruitResource {
                    @POST
                    @Transactional
                    void create(Fruit fruit) {}
                }
                """)).isEmpty();
    }
}
