package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class MissingControllerAdviceKataTest {

    private final MissingControllerAdviceKata kata = new MissingControllerAdviceKata();

    @Test
    void flagsTheFirstControllerOfAProjectWithoutAdvice() {
        assertThat(evaluate(kata, """
                @Service
                class UserService {}

                @RestController
                class UserController {}

                @Controller
                class PageController {}
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(4);
            assertThat(lesson.arguments()).containsExactly("UserController");
            assertThat(lesson.messageKey()).isEqualTo("kata.missing-controller-advice.lesson");
        });
    }

    @Test
    void doesNotAskSpringBoot2ForProblemDetail() {
        assertThat(evaluate(kata, """
                import javax.validation.Valid;

                @RestController
                class UserController {}
                """)).singleElement().satisfies(lesson ->
                assertThat(lesson.messageKey()).isEqualTo("kata.missing-controller-advice.lesson.spring-boot-2"));
    }

    @Test
    void ignoresControllersThatOnlyRenderViews() {
        assertThat(evaluate(kata, """
                @Controller
                class OwnerController {
                    @GetMapping("/owners")
                    String list(Model model) { return "owners/list"; }
                }
                """)).as("Spring Boot's error view handles them").isEmpty();
    }

    @Test
    void flagsControllersWithAResponseBody() {
        assertThat(Stream.of("""
                @Controller
                class VetController {
                    @GetMapping("/vets")
                    @ResponseBody
                    Vets list() { return null; }
                }
                """, """
                @Controller
                class VetController {
                    ResponseEntity<Vets> list() { return null; }
                }
                """, """
                @Controller
                @ResponseBody
                class VetController {}
                """)).allSatisfy(source -> assertThat(evaluate(kata, source)).as(source).hasSize(1));
    }

    @Test
    void acceptsEitherKindOfAdvice() {
        assertThat(evaluate(kata, """
                @RestController
                class UserController {}

                @org.springframework.web.bind.annotation.ControllerAdvice
                class Errors {}
                """)).isEmpty();
    }

    @Test
    void flagsTheFirstJakartaRestResourceWithoutExceptionMapper() {
        assertThat(evaluate(kata, """
                @Path("/orders")
                class OrderResource {
                    @GET
                    List<OrderResponse> list() { return List.of(); }
                }
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.arguments()).containsExactly("OrderResource");
            assertThat(lesson.messageKey()).isEqualTo("kata.missing-controller-advice.lesson.quarkus");
        });
    }

    @Test
    void acceptsExceptionMappers() {
        String resource = """
                @Path("/orders")
                class OrderResource {
                    @GET
                    List<OrderResponse> list() { return List.of(); }
                }
                """;
        assertThat(evaluate(kata, resource + """
                @Provider
                class NotFoundMapper implements ExceptionMapper<NotFoundException> {
                    public Response toResponse(NotFoundException exception) { return null; }
                }
                """)).isEmpty();
        assertThat(evaluate(kata, resource + """
                class ErrorMappers {
                    @ServerExceptionMapper
                    RestResponse<ProblemDetail> notFound(NotFoundException exception) { return null; }
                }
                """)).isEmpty();
    }

    @Test
    void doesNothingWithoutControllers() {
        assertThat(evaluate(kata, """
                @Service
                class UserService {}
                """)).isEmpty();
    }
}
