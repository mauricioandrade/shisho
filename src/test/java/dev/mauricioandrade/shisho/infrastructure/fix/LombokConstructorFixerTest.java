package dev.mauricioandrade.shisho.infrastructure.fix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static dev.mauricioandrade.shisho.infrastructure.fix.FixTestSupport.fixed;
import static org.assertj.core.api.Assertions.assertThat;

class LombokConstructorFixerTest {

    @TempDir
    Path root;

    @Test
    void writesTheConstructorLombokWouldGenerateForRequiredArgs() {
        assertThat(fixed(root, """
                package app;

                import lombok.RequiredArgsConstructor;

                @Service
                @RequiredArgsConstructor
                class OrderService {

                    private static final int LIMIT = 10;
                    private final OrderRepository orders;
                    private final Clock clock;
                    private final List<String> names = new ArrayList<>();
                    private int counter;

                    void place() {
                    }
                }
                """)).contains("""
                package app;

                @Service
                class OrderService {

                    private static final int LIMIT = 10;
                    private final OrderRepository orders;
                    private final Clock clock;
                    private final List<String> names = new ArrayList<>();
                    private int counter;

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
    void allArgsIncludesEveryInstanceFieldWithoutAFinalValue() {
        assertThat(fixed(root, """
                package app;

                @Service
                @AllArgsConstructor
                class OrderService {
                    private final OrderRepository orders;
                    private final String name = "x";
                    private int counter;
                }
                """)).hasValueSatisfying(source -> assertThat(source)
                .doesNotContain("@AllArgsConstructor")
                .contains("    public OrderService(OrderRepository orders, int counter) {"));
    }

    @Test
    void leavesClassesWhoseConstructorOtherLombokFeaturesDependOn() {
        assertThat(fixed(root, """
                package app;

                @Service
                @Builder
                @AllArgsConstructor
                class OrderService {
                    private final OrderRepository orders;
                }
                """)).isEmpty();
        assertThat(fixed(root, """
                package app;

                @Service
                @RequiredArgsConstructor(staticName = "of")
                class OrderService {
                    private final OrderRepository orders;
                }
                """)).isEmpty();
        assertThat(fixed(root, """
                package app;

                @Service
                @RequiredArgsConstructor
                class OrderService {
                    @NonNull
                    private final OrderRepository orders;
                }
                """)).isEmpty();
    }
}
