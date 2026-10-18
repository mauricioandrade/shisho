package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class EntityWithLombokDataKataTest {

    private final EntityWithLombokDataKata kata = new EntityWithLombokDataKata();

    @Test
    void flagsEntitiesWhoseEqualsUsesEveryField() {
        assertThat(evaluate(kata, """
                @Entity
                @Table(name = "account")
                @Data
                class AccountJpaEntity {
                    @Id @GeneratedValue private Long id;
                }

                @jakarta.persistence.Entity
                @lombok.EqualsAndHashCode
                class Activity {}
                """)).extracting(Lesson::line, Lesson::arguments).containsExactly(
                tuple(3, List.of("AccountJpaEntity", "Data")),
                tuple(9, List.of("Activity", "EqualsAndHashCode")));
    }

    @Test
    void acceptsChosenFieldsAndClassesThatAreNotEntities() {
        assertThat(evaluate(kata, """
                @Entity
                @Getter
                @EqualsAndHashCode(of = "code")
                class Product {}

                @Entity
                @EqualsAndHashCode(onlyExplicitlyIncluded = true)
                class Customer {}

                @Data
                class ProductResponse {}
                """)).isEmpty();
    }
}
