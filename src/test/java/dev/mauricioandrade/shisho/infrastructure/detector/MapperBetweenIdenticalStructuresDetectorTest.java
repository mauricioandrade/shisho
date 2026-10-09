package dev.mauricioandrade.shisho.infrastructure.detector;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.detector.DetectorTestSupport.project;
import static org.assertj.core.api.Assertions.assertThat;

class MapperBetweenIdenticalStructuresDetectorTest {

    private final MapperBetweenIdenticalStructuresDetector detector = new MapperBetweenIdenticalStructuresDetector();

    @Test
    void findsAMapperBetweenTypesWithTheSameFields() {
        assertThat(detector.detect(project("""
                record UserDto(Long id, String name) {}
                record User(Long id, String name) {}

                class UserMapper {
                    User toDomain(UserDto dto) { return null; }
                }
                """))).hasValueSatisfying(evidence -> assertThat(evidence.arguments()).containsExactly("UserDto", "User"));
    }

    @Test
    void acceptsMappersThatReshapeData() {
        assertThat(detector.detect(project("""
                record UserResponse(Long id, String displayName) {}
                record User(Long id, String firstName, String lastName) {}

                class UserMapper {
                    UserResponse toResponse(User user) { return null; }
                    List<UserResponse> toResponses(List<User> users) { return null; }
                }
                """))).isEmpty();
    }
}
