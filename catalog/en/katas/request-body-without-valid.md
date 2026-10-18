---
id: request-body-without-valid
---

## Why

`@NotBlank` and `@Positive` on the DTO are only metadata: nothing checks them until someone asks.
On a `@RequestBody`, the one who asks is `@Valid` (or Spring's `@Validated`). Without it, the JSON
is converted and handed to the method with its invalid fields, and the annotations give a false
sense of safety.

With `@Valid`, an invalid body becomes a `MethodArgumentNotValidException` before the method
runs, and Spring answers 400. The rule stays declared on the DTO, and the method only deals with
the valid case. Note: without `spring-boot-starter-validation` on the classpath, `@Valid` does
nothing either.

In Quarkus, with Jakarta REST, there is no `@RequestBody`: the body is the unannotated parameter of
a `@POST` or `@PUT` method, and the rule is the same, `create(@Valid CreateOrderRequest request)`.
With the `quarkus-hibernate-validator` extension, an invalid body becomes a 400 before the method
runs.

## Acceptable when

When validation is done elsewhere on purpose, as in a domain validator that has to query the
database; in that case, make it clear in the code. A DTO of the project with no constraint at all
(neither in it nor in the class it extends) is already ignored by the kata, since `@Valid` would
have nothing to check. A type from outside the project is still flagged: its annotations cannot be
seen.

## Before

```java
@RestController
class OrderController {

    @PostMapping("/orders")
    OrderResponse create(@RequestBody CreateOrderRequest request) {
        return orders.create(request);
    }
}
```

## After

```java
@RestController
class OrderController {

    @PostMapping("/orders")
    OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orders.create(request);
    }
}
```

## Before (Quarkus)

```java
@Path("/orders")
class OrderResource {

    @POST
    OrderResponse create(CreateOrderRequest request) {
        return orders.create(request);
    }
}
```

## After (Quarkus)

With the `quarkus-hibernate-validator` extension, an invalid body never reaches the method:
Quarkus answers 400 with the fields that failed.

```java
@Path("/orders")
class OrderResource {

    @POST
    OrderResponse create(@Valid CreateOrderRequest request) {
        return orders.create(request);
    }
}
```
