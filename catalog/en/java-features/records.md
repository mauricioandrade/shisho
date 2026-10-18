---
id: records
nome: Records
---

## What it is

A class that is just its data: `record Order(String customer, BigDecimal total) {}` already has a
constructor, accessors, `equals`, `hashCode` and `toString`, and its fields are `final`. It is the
natural shape of DTOs, API responses and configuration. It became final in Java 16, but the LTS
where most projects get it is 17, which is also Spring Boot 3's minimum.

## Example

```java
public record CreateOrderRequest(@NotBlank String customer, @Positive BigDecimal total) {
}
```
