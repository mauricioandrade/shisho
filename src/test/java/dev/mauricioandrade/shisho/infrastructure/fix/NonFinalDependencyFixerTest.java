package dev.mauricioandrade.shisho.infrastructure.fix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static dev.mauricioandrade.shisho.infrastructure.fix.FixTestSupport.fixed;
import static org.assertj.core.api.Assertions.assertThat;

class NonFinalDependencyFixerTest {

    @TempDir
    Path root;

    @Test
    void makesTheDependencyFinalAndKeepsTheAuthorsLayout() {
        assertThat(fixed(root, """
                package app;

                @Service
                class OrderService {

                    // the repository
                    private   OrderRepository orders;

                    OrderService(OrderRepository orders) {
                        this.orders = orders;
                    }
                }
                """)).contains("""
                package app;

                @Service
                class OrderService {

                    // the repository
                    private final   OrderRepository orders;

                    OrderService(OrderRepository orders) {
                        this.orders = orders;
                    }
                }
                """);
    }

    @Test
    void leavesFieldsThatFinalWouldBreak() {
        assertThat(fixed(root, """
                package app;

                @Service
                class OrderService {
                    private OrderRepository orders;

                    OrderService(OrderRepository orders) {
                        this.orders = orders;
                    }

                    OrderService() {
                    }

                    void replace(OrderRepository other) {
                        orders = other;
                    }
                }
                """)).isEmpty();
    }

    @Test
    void acceptsConstructorsThatDelegate() {
        assertThat(fixed(root, """
                package app;

                @Service
                class OrderService {
                    private OrderRepository orders;

                    OrderService(OrderRepository orders) {
                        this.orders = orders;
                    }

                    OrderService() {
                        this(new InMemoryOrders());
                    }
                }
                """)).hasValueSatisfying(source -> assertThat(source).contains("private final OrderRepository orders;"));
    }
}
