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

In Quarkus, the advice's role belongs to an `ExceptionMapper` or, with Quarkus REST, to
`@ServerExceptionMapper` methods in any class: each one turns an exception type into an HTTP
response. Quarkus has no ready-made `ProblemDetail`, but the RFC 9457 format is just a record with
`type`, `title`, `status` and `detail`, returned as `application/problem+json`.

## Acceptable when

In an application with few endpoints, when the default handling is enough: with
`spring.mvc.problemdetails.enabled=true`, Spring Boot already answers Spring MVC exceptions as
`ProblemDetail`. The advice becomes worth it when there are domain exceptions that need to become
specific HTTP statuses.

A `@Controller` that only returns view names (Thymeleaf, JSP) is not counted by the kata: its
errors become pages through Spring Boot's `error` view, and ProblemDetail is an API response. One
method with `@ResponseBody` or returning `ResponseEntity` is enough for it to count.

Spring Boot 2 has no `ProblemDetail`: the advice returns a record of its own with the same fields
(`status`, `title`, `detail`), and the move to Boot 3 swaps that record for the ready-made class.
The kata recognizes Boot 2 by its `javax.*` imports and does not mention `ProblemDetail` there.

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

## Before (Quarkus)

```java
@Path("/orders")
class OrderResource {

    @GET
    @Path("/{id}")
    OrderResponse find(Long id) {
        return orders.find(id);
    }
}
```

## After (Quarkus)

A single place turns exceptions into HTTP responses. In Quarkus REST, a `@ServerExceptionMapper`
method that takes the exception and returns the response is enough, here in the RFC 9457 format.

```java
@Path("/orders")
class OrderResource {

    @GET
    @Path("/{id}")
    OrderResponse find(Long id) {
        return orders.find(id);
    }
}

class ErrorHandler {

    @ServerExceptionMapper
    RestResponse<Problem> notFound(OrderNotFoundException e) {
        return RestResponse.status(Response.Status.NOT_FOUND, new Problem("Order not found", 404, e.getMessage()));
    }
}

record Problem(String title, int status, String detail) {
}
```
