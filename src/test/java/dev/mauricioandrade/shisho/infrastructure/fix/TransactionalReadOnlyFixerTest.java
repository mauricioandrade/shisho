package dev.mauricioandrade.shisho.infrastructure.fix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static dev.mauricioandrade.shisho.infrastructure.fix.FixTestSupport.fixed;
import static org.assertj.core.api.Assertions.assertThat;

class TransactionalReadOnlyFixerTest {

    @TempDir
    Path root;

    @Test
    void setsReadOnlyOnTheMethodsOwnTransaction() {
        assertThat(fixed(root, """
                package app;

                @Service
                class OrderService {

                    @Transactional
                    List<Order> findAll() { return null; }

                    @Transactional("orders")
                    Order buscarPorId(Long id) { return null; }

                    @Transactional(readOnly = false, timeout = 5)
                    long count() { return 0; }

                    @Transactional(timeout = 5)
                    boolean existsById(Long id) { return false; }
                }
                """)).contains("""
                package app;

                @Service
                class OrderService {

                    @Transactional(readOnly = true)
                    List<Order> findAll() { return null; }

                    @Transactional(value = "orders", readOnly = true)
                    Order buscarPorId(Long id) { return null; }

                    @Transactional(readOnly = true, timeout = 5)
                    long count() { return 0; }

                    @Transactional(timeout = 5, readOnly = true)
                    boolean existsById(Long id) { return false; }
                }
                """);
    }

    @Test
    void givesAMethodThatInheritsAReadWriteTransactionItsOwn() {
        assertThat(fixed(root, """
                package app;

                @Service
                @Transactional
                class OrderService {

                    List<Order> listar() { return null; }

                    void save(Order order) {}
                }
                """)).hasValueSatisfying(source -> assertThat(source)
                .contains("    @Transactional(readOnly = true)\n    List<Order> listar() { return null; }")
                .contains("    void save(Order order) {}"));
    }
}
