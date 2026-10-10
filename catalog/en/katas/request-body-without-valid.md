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

## Acceptable when

When validation is done elsewhere on purpose, as in a domain validator that has to query the
database; in that case, make it clear in the code. DTOs with no validation annotation at all are
already ignored by the kata.

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
