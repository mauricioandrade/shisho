---
id: bean-validation
nome: Bean Validation
---

## What it is

Validation rules declared as annotations on fields: `@NotBlank` (non-empty text), `@NotNull`,
`@Positive`, `@Email`, `@Size`. The rule stays next to the data, instead of spread across `if`s
in the code.

They are only checked when someone asks. In a Spring controller, the one who asks is `@Valid`
before `@RequestBody`; in a Jakarta REST resource (Quarkus), `@Valid` before the body parameter. In
both, an invalid body becomes a 400 before the method runs.

## Example

```java
record CreateOrderRequest(@NotBlank String customer, @Positive int quantity) {
}

@RestController
class OrderController {

    @PostMapping("/orders")
    OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return service.create(request);
    }
}
```
