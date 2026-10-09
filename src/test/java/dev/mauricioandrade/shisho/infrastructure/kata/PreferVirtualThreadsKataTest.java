package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class PreferVirtualThreadsKataTest {

    private final PreferVirtualThreadsKata kata = new PreferVirtualThreadsKata();

    @Test
    void suggestsVirtualThreadsForPlatformPools() {
        assertThat(evaluate(kata, """
                class Notifications {
                    private final ExecutorService fixed = Executors.newFixedThreadPool(20);
                    private final ExecutorService cached = java.util.concurrent.Executors.newCachedThreadPool();
                }
                """)).extracting(lesson -> lesson.arguments().getFirst()).containsExactly("newFixedThreadPool", "newCachedThreadPool");
    }

    @Test
    void acceptsVirtualAndScheduledExecutors() {
        assertThat(evaluate(kata, """
                class Notifications {
                    private final ExecutorService virtual = Executors.newVirtualThreadPerTaskExecutor();
                    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
                    private final Pool pool = myPools.newFixedThreadPool(2);
                }
                """)).isEmpty();
    }
}
