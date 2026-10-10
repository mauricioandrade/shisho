---
id: strategy
nome: Strategy
---

## What it is

Several ways of doing the same thing, each in its own class, all behind the same interface.
Whoever uses it only knows the interface and needs no `if`/`switch` to pick the behavior: it
receives the right implementation, often injected by Spring.

Adding a new behavior means writing one more class, without touching the others. With a single
implementation, the interface is not a Strategy yet: it is an indirection waiting for a reason.

## Example

```java
interface ShippingCost {
    BigDecimal calculate(Order order);
}

class ExpressShipping implements ShippingCost {
    public BigDecimal calculate(Order order) {
        return order.weight().multiply(new BigDecimal("5"));
    }
}

class EconomyShipping implements ShippingCost {
    public BigDecimal calculate(Order order) {
        return order.weight().multiply(new BigDecimal("2"));
    }
}
```
