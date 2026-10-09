package dev.mauricioandrade.shisho.infrastructure.detector;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.detector.DetectorTestSupport.project;
import static org.assertj.core.api.Assertions.assertThat;

class DomainEntityIdenticalToJpaEntityDetectorTest {

    private final DomainEntityIdenticalToJpaEntityDetector detector = new DomainEntityIdenticalToJpaEntityDetector();

    @Test
    void findsAJpaCopyOfADomainType() {
        assertThat(detector.detect(project("""
                record User(Long id, String name) {}

                @Entity
                class UserJpaEntity {
                    @Id private Long id;
                    private String name;
                    private static final long serialVersionUID = 1L;
                }
                """))).hasValueSatisfying(evidence -> assertThat(evidence.arguments()).containsExactly("User", "UserJpaEntity"));
    }

    @Test
    void acceptsEntitiesThatDifferFromTheDomain() {
        assertThat(detector.detect(project("""
                record User(Long id, String name) {}

                @Entity
                class UserEntity {
                    @Id private Long id;
                    private String name;
                    private Instant createdAt;
                }

                @Entity
                class Order {
                    @Id private Long id;
                }
                """))).isEmpty();
    }
}
