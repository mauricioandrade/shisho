---
id: injecao-de-dependencia
nome: Constructor dependency injection
---

## What it is

The class does not create what it needs: it receives it ready, through the constructor. Spring is
the one who creates and hands it over, looking up a bean of each requested type. With a single
constructor, there is no need for `@Autowired`.

The gain is decoupling: the class depends on a type, not on how it is built. In a test, you pass
a fake instead of the real thing with a plain `new`, without starting Spring.

## Example

```java
@Service
class OrderClosing {

    private final OrderRepository orders;
    private final ShippingCost shipping;

    OrderClosing(OrderRepository orders, ShippingCost shipping) {
        this.orders = orders;
        this.shipping = shipping;
    }
}
```
