package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class EntityInControllerKataTest {

    private static final String ENTITIES = """
            @jakarta.persistence.Entity
            class User {}

            @Entity
            class Order {}
            """;

    private final EntityInControllerKata kata = new EntityInControllerKata();

    @Test
    void flagsEntitiesReturnedOrReceivedByEndpoints() {
        assertThat(evaluate(kata, ENTITIES + """
                @RestController
                class UserController {
                    @GetMapping
                    ResponseEntity<List<User>> list() { return null; }

                    @PostMapping
                    void create(@Valid @RequestBody User user) {}

                    @PutMapping
                    Order update(@RequestBody Order order) { return order; }
                }
                """)).satisfiesExactly(
                list -> {
                    assertThat(list.line()).isEqualTo(8);
                    assertThat(list.arguments()).containsExactly("User", "list");
                    assertThat(list.messageKey()).isEqualTo("kata.entity-in-controller.lesson");
                    assertThat(list.file().toString()).endsWith("Sample.java");
                },
                create -> assertThat(create.arguments()).containsExactly("User", "create"),
                update -> {
                    assertThat(update.line()).isEqualTo(14);
                    assertThat(update.arguments()).containsExactly("Order", "update");
                });
    }

    @Test
    void acceptsDtosPathVariablesAndHelpersOutsideMappings() {
        assertThat(evaluate(kata, ENTITIES + """
                @RestController
                class UserController {
                    @GetMapping("/{id}")
                    UserResponse find(@PathVariable User id) { return null; }

                    @PostMapping
                    UserResponse create(@Valid @RequestBody CreateUserRequest request) { return null; }

                    private User toEntity(CreateUserRequest request) { return null; }
                }

                @Service
                class UserService {
                    @GetMapping
                    User find() { return null; }
                }
                """)).isEmpty();
    }

    @Test
    void doesNothingWithoutEntities() {
        assertThat(evaluate(kata, """
                @RestController
                class UserController {
                    @GetMapping
                    User find() { return null; }
                }
                """)).isEmpty();
    }
}
