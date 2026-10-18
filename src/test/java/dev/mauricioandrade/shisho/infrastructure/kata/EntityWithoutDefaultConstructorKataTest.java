package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class EntityWithoutDefaultConstructorKataTest {

    private final EntityWithoutDefaultConstructorKata kata = new EntityWithoutDefaultConstructorKata();

    @Test
    void flagsMappedClassesWhoseConstructorsAllTakeParameters() {
        assertThat(evaluate(kata, """
                @Entity
                class Product {
                    Product(String name) {}
                }

                @Embeddable
                class Address {
                    Address(String street) {}
                    Address(String street, String city) {}
                }
                """)).extracting(lesson -> lesson.arguments().getFirst()).containsExactly("Product", "Address");
    }

    @Test
    void acceptsADefaultConstructorLombokOrRecords() {
        assertThat(evaluate(kata, """
                @Entity
                class Product {}

                @Entity
                class Order {
                    protected Order() {}
                    Order(String customer) {}
                }

                @Entity
                @NoArgsConstructor(access = AccessLevel.PROTECTED)
                class Customer {
                    Customer(String name) {}
                }

                @Embeddable
                record Money(long cents) {}

                class ProductRequest {
                    ProductRequest(String name) {}
                }
                """)).isEmpty();
    }
}
