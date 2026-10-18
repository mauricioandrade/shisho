package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class UnproxyableMethodKataTest {

    private final UnproxyableMethodKata kata = new UnproxyableMethodKata();

    @Test
    void flagsProxiedAnnotationsOnMethodsTheProxyCannotReach() {
        assertThat(evaluate(kata, """
                @Service
                class OrderService {
                    @Transactional
                    private void save() {}

                    @Async
                    static void notifyAll() {}

                    @org.springframework.cache.annotation.Cacheable("orders")
                    public final Order find(Long id) { return null; }
                }
                """)).extracting(Lesson::line, Lesson::arguments, Lesson::messageKey).containsExactly(
                tuple(3, List.of("Transactional", "save", "private"), "kata.unproxyable-method.lesson"),
                tuple(6, List.of("Async", "notifyAll", "static"), "kata.unproxyable-method.lesson"),
                tuple(9, List.of("Cacheable", "find", "final"), "kata.unproxyable-method.lesson"));
    }

    @Test
    void acceptsPublicAndOnSpring6NonPublicMethods() {
        assertThat(evaluate(kata, """
                import jakarta.persistence.Entity;

                @Service
                class OrderService {
                    @Transactional
                    public void save() {}

                    @Transactional
                    void archive() {}

                    @Transactional
                    protected void purge() {}

                    private void helper() {}
                }

                interface Orders {
                    @Transactional
                    void save();
                }
                """)).isEmpty();
    }

    @Test
    void onSpringBoot2OnlyPublicMethodsAreReached() {
        assertThat(evaluate(kata, """
                import javax.persistence.Entity;

                @Service
                class OrderService {
                    @Transactional
                    void archive() {}

                    @Transactional
                    protected void purge() {}

                    @Transactional
                    public void save() {}
                }
                """)).extracting(lesson -> lesson.arguments().get(2), Lesson::messageKey).containsExactly(
                tuple("package-private", "kata.unproxyable-method.lesson.spring-boot-2"),
                tuple("protected", "kata.unproxyable-method.lesson.spring-boot-2"));
    }

    @Test
    void doesNotRunOnQuarkus() {
        assertThat(evaluate(kata, """
                @ApplicationScoped
                class OrderService {
                    @Transactional
                    private void save() {}
                }
                """, "io.quarkus:quarkus-arc")).as("Quarkus 3 fails the build instead").isEmpty();
    }
}
