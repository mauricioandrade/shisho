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
    void flagsCallsFromInsideTheClassThatSkipTheProxy() {
        assertThat(evaluate(kata, """
                @Service
                class OrderService {
                    OrderService() {
                        warmUp();
                    }

                    public void close(Order order) {
                        this.save(order);
                        notifyCustomer(order);
                        order.items().forEach(this::audit);
                    }

                    @Transactional
                    public void save(Order order) {}

                    @Async
                    public void notifyCustomer(Order order) {}

                    @Transactional
                    public void audit(Item item) {}

                    @Cacheable("warm")
                    public void warmUp() {}
                }
                """)).extracting(Lesson::line, Lesson::arguments, Lesson::messageKey).containsExactly(
                tuple(4, List.of("Cacheable", "warmUp"), "kata.unproxyable-method.lesson.self-invocation"),
                tuple(8, List.of("Transactional", "save"), "kata.unproxyable-method.lesson.self-invocation"),
                tuple(9, List.of("Async", "notifyCustomer"), "kata.unproxyable-method.lesson.self-invocation"),
                tuple(10, List.of("Transactional", "audit"), "kata.unproxyable-method.lesson.self-invocation"));
    }

    @Test
    void aTransactionTheCallerRunsInCoversTheCallee() {
        assertThat(evaluate(kata, """
                @Service
                class OrderService {
                    @Transactional
                    public void close(Order order) {
                        save(order);
                        log(order);
                    }

                    @Transactional
                    public void save(Order order) {}

                    @Transactional(propagation = Propagation.REQUIRES_NEW)
                    public void log(Order order) {}
                }

                @Service
                @Transactional
                class PaymentService {
                    public void pay() { charge(); }

                    @Transactional
                    public void charge() {}
                }
                """)).extracting(Lesson::line, lesson -> lesson.arguments().get(1)).as("REQUIRES_NEW is never honored from inside")
                .containsExactly(tuple(6, "log"));
    }

    @Test
    void tellsOverloadsApartAndLeavesAmbiguousCallsAlone() {
        assertThat(evaluate(kata, """
                @Service
                class OrderService {
                    public void close(Order order) {
                        save(order);
                        save(order, true);
                        orders.forEach(this::save);
                        repository.save(order);
                        archive();
                    }

                    @Transactional
                    public void save(Order order) {}

                    public void save(Order order, boolean flush) {}

                    @Transactional
                    private void archive() {}
                }
                """)).extracting(Lesson::line, Lesson::messageKey)
                .as("save(order) reaches the annotated overload; this::save could be either; archive is private, the first rule's")
                .containsExactly(tuple(16, "kata.unproxyable-method.lesson"), tuple(4, "kata.unproxyable-method.lesson.self-invocation"));
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
