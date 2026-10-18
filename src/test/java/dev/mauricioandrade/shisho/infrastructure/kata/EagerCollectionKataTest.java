package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class EagerCollectionKataTest {

    private final EagerCollectionKata kata = new EagerCollectionKata();

    @Test
    void flagsEagerCollectionsOnFieldsAndGetters() {
        assertThat(evaluate(kata, """
                @Entity
                class Owner {
                    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
                    private Set<Pet> pets;

                    @ManyToMany(fetch = EAGER)
                    public Set<Role> getRoles() { return null; }
                }
                """)).extracting(Lesson::line, Lesson::arguments).containsExactly(
                tuple(3, List.of("pets", "OneToMany")),
                tuple(6, List.of("getRoles", "ManyToMany")));
    }

    @Test
    void acceptsLazyAndDefaultCollections() {
        assertThat(evaluate(kata, """
                @Entity
                class Owner {
                    @OneToMany(mappedBy = "owner")
                    private Set<Pet> pets;

                    @ManyToMany(fetch = FetchType.LAZY)
                    private Set<Role> roles;

                    @ManyToOne(fetch = FetchType.EAGER)
                    private City city;
                }
                """)).as("a to-one is the other kata's").isEmpty();
    }
}
