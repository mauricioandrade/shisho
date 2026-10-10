---
id: beans
nome: Spring beans and stereotypes
---

## What it is

A bean is an object Spring creates and manages for you. Annotations such as `@Service`,
`@Repository` and `@RestController` (the stereotypes) mark the class: at startup, Spring finds it,
creates one instance and hands it to whoever needs it.

The stereotype also states the class's role: `@Service` for business rules, `@Repository` for
data access, `@RestController` for the HTTP door. By default each bean is a singleton shared by
every request, which is why it should not keep any request's state.

## Example

```java
@Service
class ShippingCost {

    BigDecimal calculate(Order order) {
        return order.weight().multiply(new BigDecimal("2.5"));
    }
}
```
