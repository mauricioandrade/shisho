package dev.mauricioandrade.shisho.infrastructure.detector;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static dev.mauricioandrade.shisho.infrastructure.detector.DetectorTestSupport.project;
import static org.assertj.core.api.Assertions.assertThat;

class AnemicDomainDetectorTest {

    private final AnemicDomainDetector detector = new AnemicDomainDetector();

    @Test
    void findsDomainClassesWithOnlyAccessors() {
        assertThat(detector.detect(project(Map.of(
                "shop/domain/Order.java", """
                        package shop.domain;
                        class Order {
                            private Long id;
                            Long getId() { return id; }
                            void setId(Long id) { this.id = id; }
                        }
                        """,
                "shop/domain/Customer.java", """
                        package shop.domain;
                        class Customer {
                            private String name;
                            String getName() { return name; }
                            public String toString() { return name; }
                        }
                        """,
                "shop/domain/Invoice.java", """
                        package shop.domain;
                        class Invoice {
                            private long total;
                            void pay() {}
                        }
                        """)))).hasValueSatisfying(evidence -> assertThat(evidence.arguments()).containsExactly(2L, 3));
    }

    @Test
    void acceptsADomainWithBehaviorAndRecordsAndIgnoresOtherPackages() {
        assertThat(detector.detect(project(Map.of(
                "shop/domain/Order.java", """
                        package shop.domain;
                        class Order {
                            private Long id;
                            Long getId() { return id; }
                            void cancel() {}
                        }
                        record Money(long cents) {}
                        record OrderId(long value) {}
                        """,
                "shop/web/OrderResponse.java", """
                        package shop.web;
                        class OrderResponse {
                            private Long id;
                            Long getId() { return id; }
                        }
                        class CustomerResponse {
                            private Long id;
                            Long getId() { return id; }
                        }
                        """)))).isEmpty();
    }
}
