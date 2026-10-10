---
id: missing-controller-advice
---

## Why

Without a central place, each controller decides on its own how to answer an error: one returns
`500` with the exception's stack trace, another its own JSON, another forgets. API consumers have
to handle different formats, and internal details may leak in the response.

A `@RestControllerAdvice` turns exceptions into HTTP responses in a single place. Since Spring 6,
the standard format is RFC 9457's `ProblemDetail` (`application/problem+json`), and extending
`ResponseEntityExceptionHandler` already handles Spring MVC's own exceptions, such as validation
ones.

## Acceptable when

In an application with few endpoints, when the default handling is enough: with
`spring.mvc.problemdetails.enabled=true`, Spring Boot already answers Spring MVC exceptions as
`ProblemDetail`. The advice becomes worth it when there are domain exceptions that need to become
specific HTTP statuses.

## Before

```java
@RestController
class OrderController {

    @GetMapping("/orders/{id}")
    OrderResponse find(@PathVariable Long id) {
        return orders.find(id);
    }
}
```

## After

A single place turns exceptions into HTTP responses, in the standard `application/problem+json`
format.

```java
@RestController
class OrderController {

    @GetMapping("/orders/{id}")
    OrderResponse find(@PathVariable Long id) {
        return orders.find(id);
    }
}

@RestControllerAdvice
class ErrorHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail notFound(OrderNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
```
