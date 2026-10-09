package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class PreferScopedValuesKataTest {

    private final PreferScopedValuesKata kata = new PreferScopedValuesKata();

    @Test
    void suggestsScopedValuesForThreadLocalFields() {
        assertThat(evaluate(kata, """
                class Context {
                    private static final ThreadLocal<User> USER = new ThreadLocal<>();
                    private static final InheritableThreadLocal<String> TENANT = new InheritableThreadLocal<>();
                    private static final java.lang.ThreadLocal<Locale> LOCALE = new ThreadLocal<>();
                }
                """)).extracting(lesson -> lesson.arguments().getFirst()).containsExactly("USER", "TENANT", "LOCALE");
    }

    @Test
    void acceptsScopedValuesAndLookalikes() {
        assertThat(evaluate(kata, """
                class Context {
                    private static final ScopedValue<User> USER = ScopedValue.newInstance();
                    private final ThreadLocalRandomizer randomizer = new ThreadLocalRandomizer();
                }
                """)).isEmpty();
    }
}
