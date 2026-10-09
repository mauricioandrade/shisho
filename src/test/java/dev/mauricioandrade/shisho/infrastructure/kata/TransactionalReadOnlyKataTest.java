package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionalReadOnlyKataTest {

    private final TransactionalReadOnlyKata kata = new TransactionalReadOnlyKata();

    @Test
    void flagsReadMethodsInReadWriteTransactions() {
        assertThat(evaluate(kata, """
                @Service
                class UserService {
                    @Transactional
                    List<User> findAll() { return null; }

                    @Transactional(timeout = 5)
                    User buscarPorId(Long id) { return null; }

                    @Transactional(readOnly = false)
                    long count() { return 0; }
                }
                """)).satisfiesExactly(
                findAll -> {
                    assertThat(findAll.line()).isEqualTo(3);
                    assertThat(findAll.arguments()).containsExactly("findAll");
                    assertThat(findAll.messageKey()).isEqualTo("kata.transactional-read-only.lesson");
                },
                buscar -> assertThat(buscar.arguments()).containsExactly("buscarPorId"),
                count -> assertThat(count.arguments()).containsExactly("count"));
    }

    @Test
    void flagsReadMethodsInheritingAReadWriteClassTransaction() {
        assertThat(evaluate(kata, """
                @Service
                @Transactional
                class UserService {
                    List<User> listar() { return null; }

                    void save(User user) {}
                }
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(4);
            assertThat(lesson.arguments()).containsExactly("listar");
        });
    }

    @Test
    void acceptsReadOnlyTransactionsWritesAndMethodsWithoutTransaction() {
        assertThat(evaluate(kata, """
                @Service
                @Transactional(readOnly = true)
                class UserService {
                    List<User> findAll() { return null; }

                    @Transactional
                    void save(User user) {}

                    @Transactional
                    void getaway() {}

                    @Transactional
                    void listen() {}
                }

                class Report {
                    List<User> findAll() { return null; }
                }
                """)).isEmpty();
    }

    @Test
    void skipsJtaTransactionsWhichHaveNoReadOnly() {
        assertThat(evaluate(kata, """
                import jakarta.transaction.Transactional;

                class UserService {
                    @Transactional
                    List<User> findAll() { return null; }
                }
                """)).isEmpty();
    }
}
