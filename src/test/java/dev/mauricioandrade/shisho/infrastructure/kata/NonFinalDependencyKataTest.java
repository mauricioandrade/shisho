package dev.mauricioandrade.shisho.infrastructure.kata;

import dev.mauricioandrade.shisho.domain.model.Lesson;
import org.junit.jupiter.api.Test;

import static dev.mauricioandrade.shisho.infrastructure.kata.KataTestSupport.evaluate;
import static org.assertj.core.api.Assertions.assertThat;

class NonFinalDependencyKataTest {

    private final NonFinalDependencyKata kata = new NonFinalDependencyKata();

    @Test
    void flagsNonFinalFieldReceivedThroughTheConstructor() {
        assertThat(evaluate(kata, """
                @Service
                class OrderService {
                    private OrderRepository repository;

                    OrderService(OrderRepository repository) {
                        this.repository = repository;
                    }
                }
                """)).singleElement().satisfies(lesson -> {
            assertThat(lesson.line()).isEqualTo(3);
            assertThat(lesson.arguments()).containsExactly("repository");
        });
    }

    @Test
    void flagsEveryMutableFieldWithAllArgsConstructor() {
        assertThat(evaluate(kata, """
                @RestController
                @AllArgsConstructor
                class OrderController {
                    private OrderService service;
                    private Clock clock;
                    private static Logger log;
                    private int retries = 3;
                }
                """)).extracting(lesson -> lesson.arguments().getFirst()).containsExactly("service", "clock");
    }

    @Test
    void acceptsFinalDependencies() {
        assertThat(evaluate(kata, """
                @Service
                class OrderService {
                    private final OrderRepository repository;

                    OrderService(OrderRepository repository) {
                        this.repository = repository;
                    }
                }
                """)).isEmpty();
    }

    @Test
    void leavesAutowiredAndValueFieldsToTheirOwnKatas() {
        assertThat(evaluate(kata, """
                @Component
                @AllArgsConstructor
                class Mailer {
                    @Autowired
                    private MailClient client;
                    @Value("${mail.host}")
                    private String host;
                }
                """)).isEmpty();
    }

    @Test
    void ignoresClassesThatAreNotSpringBeans() {
        assertThat(evaluate(kata, """
                class Order {
                    private Status status;

                    Order(Status status) {
                        this.status = status;
                    }

                    void cancel() {
                        status = Status.CANCELLED;
                    }
                }
                """)).extracting(Lesson::kataId).isEmpty();
    }
}
