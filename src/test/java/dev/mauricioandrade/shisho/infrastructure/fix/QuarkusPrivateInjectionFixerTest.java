package dev.mauricioandrade.shisho.infrastructure.fix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static dev.mauricioandrade.shisho.infrastructure.fix.FixTestSupport.fix;
import static org.assertj.core.api.Assertions.assertThat;

class QuarkusPrivateInjectionFixerTest {

    private static final String QUARKUS_POM = """
            <project>
                <properties><maven.compiler.release>21</maven.compiler.release></properties>
                <dependencies>
                    <dependency>
                        <groupId>io.quarkus</groupId>
                        <artifactId>quarkus-arc</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """;

    @TempDir
    Path root;

    @Test
    void leavesTheInjectedFieldPackagePrivate() {
        assertThat(fix(root, QUARKUS_POM, """
                package app;

                @ApplicationScoped
                class OrderService {

                    @Inject
                    private OrderRepository orders;

                    @ConfigProperty(name = "orders.limit")
                    private int limit;
                }
                """)).singleElement().satisfies(fix -> assertThat(fix.fixed()).contains("""
                    @Inject
                    OrderRepository orders;

                    @ConfigProperty(name = "orders.limit")
                    int limit;
                """));
    }
}
