package dev.mauricioandrade.shisho.infrastructure.kata;

import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class RequestBodyWithoutValidKataTest {

    private final RequestBodyWithoutValidKata kata = new RequestBodyWithoutValidKata();

    @Test
    void flagsRequestBodyWithoutValidation() {
        assertThat(evaluate(kata, """
                @RestController
                class OrderController {
                    @PostMapping
                    void create(
                            @RequestBody CreateOrderRequest request) {}
                }
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(5);
            assertThat(lesson.arguments()).containsExactly("CreateOrderRequest", "jakarta.validation.Valid");
        });
    }

    @Test
    void namesTheJavaxValidOfASpringBoot2Project() {
        assertThat(evaluate(kata, """
                import javax.persistence.Entity;

                @RestController
                class OrderController {
                    void create(@RequestBody CreateOrderRequest request) {}
                }
                """)).singleElement().satisfies(lesson ->
                assertThat(lesson.arguments()).containsExactly("CreateOrderRequest", "javax.validation.Valid"));
    }

    @Test
    void acceptsValidOrValidated() {
        assertThat(evaluate(kata, """
                @RestController
                class OrderController {
                    void create(@Valid @RequestBody CreateOrderRequest request) {}
                    void update(@RequestBody @Validated UpdateOrderRequest request) {}
                }
                """)).isEmpty();
    }

    @Test
    void flagsJakartaRestBodiesWithoutValidation() {
        assertThat(evaluate(kata, """
                @Path("/orders/{tenant}")
                class OrderResource {
                    @POST
                    @Path("{id}")
                    void create(String tenant, Long id, @RestQuery String source, UriInfo uri,
                            CreateOrderRequest request) {}

                    @PUT
                    void update(@Valid UpdateOrderRequest request) {}

                    @GET
                    List<Order> list(@QueryParam("page") int page) { return List.of(); }
                }
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(6);
            assertThat(lesson.arguments()).containsExactly("CreateOrderRequest", "jakarta.validation.Valid");
            assertThat(lesson.messageKey()).isEqualTo("kata.request-body-without-valid.lesson.quarkus");
        });
    }

    @Test
    void ignoresBodiesWithNothingToValidate() {
        assertThat(evaluate(kata, """
                @RestController
                class WebhookController {
                    void raw(@RequestBody String payload) {}
                    void bytes(@RequestBody byte[] payload) {}
                    void map(@RequestBody Map<String, Object> payload) {}
                    void ids(@RequestBody Set<Long> ids) {}
                    void names(@RequestBody java.util.List<String> names) {}
                    void array(@RequestBody Long[] ids) {}
                    void upload(@RequestBody MultipartFile file) {}
                }
                """)).isEmpty();
    }

    @Test
    void ignoresProjectBodiesWithoutConstraints() {
        assertThat(evaluate(kata, """
                @RestController
                class AccountController {
                    void changePassword(@RequestBody PasswordChange change) {}
                    void changeAll(@RequestBody List<PasswordChange> changes) {}
                }

                class PasswordChange {
                    private String currentPassword;
                    private String newPassword;
                }
                """)).as("@Valid would check nothing").isEmpty();
    }

    @Test
    void flagsProjectBodiesWithConstraints() {
        assertThat(evaluate(kata, """
                @RestController
                class OrderController {
                    void create(@RequestBody CreateOrder order) {}
                    void rename(@RequestBody Rename rename) {}
                    void register(@RequestBody ManagedUser user) {}
                }

                record CreateOrder(@jakarta.validation.constraints.NotBlank String customer) {}

                class Rename {
                    @ValidName String name;
                }

                @Constraint(validatedBy = NameValidator.class)
                @interface ValidName {}

                class ManagedUser extends User {}

                class User {
                    @javax.validation.constraints.Email String email;
                }
                """)).extracting(lesson -> lesson.arguments().getFirst())
                .as("a fully qualified constraint, one of the project, or one inherited")
                .containsExactly("CreateOrder", "Rename", "ManagedUser");
    }

    @Test
    void aFileImportingBeanValidationHasConstraints() {
        assertThat(evaluate(kata, """
                import jakarta.validation.constraints.*;

                @RestController
                class OrderController {
                    void create(@RequestBody CreateOrder order) {}
                }

                class CreateOrder {
                    @NotBlank String customer;
                }
                """)).hasSize(1);
    }
}
