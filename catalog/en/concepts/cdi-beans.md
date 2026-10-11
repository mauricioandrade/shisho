---
id: cdi-beans
nome: CDI beans and scopes
---

## What it is

In Quarkus, beans belong to CDI (Jakarta Contexts and Dependency Injection), implemented by ArC.
The scope annotation says how long the object lives: `@ApplicationScoped`, one per application,
created on first use; `@RequestScoped`, one per request; `@Singleton`, one per application, with
no proxy; `@Dependent`, a new one for every place that injects it.

Whoever needs the bean receives it with `@Inject`, on a package-private field or in the
constructor. The difference from Spring is the moment: ArC discovers the beans and wires the
injection at build time, not when the application starts.

## Example

```java
@ApplicationScoped
class ShippingCalculator {

    @Inject
    PriceTable prices;

    BigDecimal calculate(Order order) {
        return order.weight().multiply(prices.perKilo());
    }
}
```
