package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class DtoNotRecordKataTest {

    private final DtoNotRecordKata kata = new DtoNotRecordKata();

    @Test
    void flagsClassesNamedLikeDtos() {
        assertThat(evaluate(kata, """
                class CreateUserRequest {}
                class UserResponse {}
                class UserDto {}
                class UserDTO {}
                """)).extracting(lesson -> lesson.arguments().getFirst())
                .containsExactly("CreateUserRequest", "UserResponse", "UserDto", "UserDTO");
    }

    @Test
    void flagsClassesInDtoPackages() {
        assertThat(evaluate(kata, """
                package app.api.dto;

                class Address { private String street; }
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(3);
            assertThat(lesson.arguments()).containsExactly("Address");
        });
    }

    @Test
    void acceptsRecordsAndNonDtoTypes() {
        assertThat(evaluate(kata, """
                package app.api.response;

                record UserResponse(String name) {}
                interface PageResponse {}
                enum StatusDto { ACTIVE }
                """)).extracting(Lesson::kataId).isEmpty();
    }

    @Test
    void ignoresBuildersAndStatelessClassesInDtoPackages() {
        assertThat(evaluate(kata, """
                package app.api.dto;

                record PetDetails(String name) {
                    static final class PetDetailsBuilder { private String name; }
                }
                class UserDtoBuilder { private String name; }
                class UserMapper {
                    static PetDetails toDetails(Object pet) { return null; }
                }
                """)).as("a builder is mutable by design; a mapper carries no data").isEmpty();
    }

    @Test
    void ignoresOrdinaryClassesElsewhere() {
        assertThat(evaluate(kata, """
                package app.service;

                class UserService {}
                class RequestCounter {}
                """)).isEmpty();
    }
}
