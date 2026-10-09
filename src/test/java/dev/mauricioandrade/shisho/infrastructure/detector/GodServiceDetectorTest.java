package dev.mauricioandrade.shisho.infrastructure.detector;

import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.mauricioandrade.shisho.infrastructure.detector.DetectorTestSupport.project;
import static org.assertj.core.api.Assertions.assertThat;

class GodServiceDetectorTest {

    private final GodServiceDetector detector = new GodServiceDetector();

    @Test
    void findsAServiceWithTooManyMethods() {
        assertThat(detector.detect(project("@Service class OrderService {" + methods(20) + "}")))
                .hasValueSatisfying(evidence -> assertThat(evidence.arguments()).containsExactly("OrderService", 20, 0));
    }

    @Test
    void findsAServiceWithTooManyDependencies() {
        assertThat(detector.detect(project("""
                @Service
                class OrderService {
                    OrderService(A a, B b, C c, D d, E e, F f, G g, H h) {}
                }
                """))).hasValueSatisfying(evidence -> assertThat(evidence.arguments()).containsExactly("OrderService", 0, 8));
    }

    @Test
    void acceptsSmallServicesAndLargeNonServices() {
        assertThat(detector.detect(project("@Service class OrderService {" + methods(19) + "}"
                + " class Report {" + methods(40) + "}"))).isEmpty();
    }

    private static String methods(int count) {
        return IntStream.range(0, count).mapToObj(i -> " void m" + i + "() {}").collect(Collectors.joining());
    }
}
