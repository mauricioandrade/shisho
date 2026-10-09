package dev.mauricioandrade.shisho.infrastructure.detector;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.detector.DetectorTestSupport.project;
import static org.assertj.core.api.Assertions.assertThat;

class CyclicServiceDependencyDetectorTest {

    private final CyclicServiceDependencyDetector detector = new CyclicServiceDependencyDetector();

    @Test
    void findsTwoServicesThatDependOnEachOther() {
        assertThat(detector.detect(project("""
                @Service
                class OrderService {
                    OrderService(CustomerService customers, OrderRepository orders) {}
                }

                @Service
                class CustomerService {
                    @Autowired
                    private OrderService orders;
                }
                """))).hasValueSatisfying(evidence ->
                assertThat(evidence.arguments()).containsExactly("CustomerService", "OrderService"));
    }

    @Test
    void acceptsOneWayDependencies() {
        assertThat(detector.detect(project("""
                @Service
                class OrderService {
                    OrderService(CustomerService customers) {}
                }

                @Service
                class CustomerService {
                    CustomerService(CustomerRepository customers) {}
                }
                """))).isEmpty();
    }
}
