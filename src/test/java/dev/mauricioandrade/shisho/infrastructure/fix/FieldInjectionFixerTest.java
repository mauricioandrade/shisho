package dev.mauricioandrade.shisho.infrastructure.fix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static dev.mauricioandrade.shisho.infrastructure.fix.FixTestSupport.fixed;
import static org.assertj.core.api.Assertions.assertThat;

class FieldInjectionFixerTest {

    @TempDir
    Path root;

    @Test
    void movesInjectedFieldsToANewConstructorAfterTheFields() {
        assertThat(fixed(root, """
                package app;

                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.stereotype.Service;

                @Service
                public class OrderService {

                    @Autowired
                    private OrderRepository orders;

                    @Autowired
                    private Clock clock;

                    void place() {
                    }
                }
                """)).contains("""
                package app;

                import org.springframework.stereotype.Service;

                @Service
                public class OrderService {

                    private final OrderRepository orders;

                    private final Clock clock;

                    public OrderService(OrderRepository orders, Clock clock) {
                        this.orders = orders;
                        this.clock = clock;
                    }

                    void place() {
                    }
                }
                """);
    }

    @Test
    void addsToTheSingleExistingConstructor() {
        assertThat(fixed(root, """
                package app;

                @Service
                class OrderService {
                    private final Clock clock;
                    @Inject
                    private OrderRepository orders;

                    OrderService(Clock clock) {
                        this.clock = clock;
                    }
                }
                """)).hasValueSatisfying(source -> assertThat(source)
                .contains("    private final OrderRepository orders;\n")
                .contains("    OrderService(Clock clock, OrderRepository orders) {\n        this.clock = clock;\n        this.orders = orders;\n    }"));
    }

    @Test
    void leavesWhatWouldChangeTheMeaning() {
        assertThat(fixed(root, """
                package app;

                @Service
                class OrderService {
                    @Autowired(required = false)
                    private OrderRepository orders;
                }
                """)).isEmpty();
        assertThat(fixed(root, """
                package app;

                @Service
                class OrderService {
                    @Autowired
                    @Qualifier("primary")
                    private OrderRepository orders;
                }
                """)).isEmpty();
        assertThat(fixed(root, """
                package app;

                @Service
                class OrderService {
                    @Autowired
                    private OrderRepository orders;

                    OrderService() {
                    }

                    OrderService(int size) {
                    }
                }
                """)).isEmpty();
    }
}
