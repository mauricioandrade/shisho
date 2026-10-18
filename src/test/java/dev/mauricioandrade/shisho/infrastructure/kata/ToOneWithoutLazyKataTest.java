package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ToOneWithoutLazyKataTest {

    private final ToOneWithoutLazyKata kata = new ToOneWithoutLazyKata();

    @Test
    void flagsToOneRelationshipsLeftToTheEagerDefault() {
        assertThat(evaluate(kata, """
                @Entity
                class Visit {
                    @ManyToOne
                    @JoinColumn(name = "pet_id")
                    private Pet pet;

                    @OneToOne(cascade = CascadeType.ALL)
                    private Invoice invoice;
                }
                """)).extracting(Lesson::line, Lesson::arguments).containsExactly(
                tuple(3, List.of("pet", "ManyToOne")),
                tuple(7, List.of("invoice", "OneToOne")));
    }

    @Test
    void acceptsAnExplicitFetchAndTheInverseSideOfAOneToOne() {
        assertThat(evaluate(kata, """
                @Entity
                class Visit {
                    @ManyToOne(fetch = FetchType.LAZY)
                    private Pet pet;

                    @ManyToOne(fetch = FetchType.EAGER)
                    private Vet vet;

                    @OneToOne(mappedBy = "visit")
                    private Invoice invoice;

                    @OneToMany(mappedBy = "visit")
                    private List<Note> notes;
                }
                """)).isEmpty();
    }
}
