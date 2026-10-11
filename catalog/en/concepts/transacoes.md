---
id: transacoes
nome: Transactions (@Transactional)
---

## What it is

A transaction groups database operations that must happen together: either all of them count, or
none does. With `@Transactional`, the framework opens the transaction before the method and
commits at the end, or rolls back if an unchecked exception escapes.

In Spring, `readOnly = true` says the method only reads, and Hibernate saves work; the Jakarta
`@Transactional`, which Quarkus uses, has no such option. In both, the annotation only applies when
the method is called from outside the class, because what applies it is a proxy around the bean.

## Example

```java
@Service
@Transactional(readOnly = true)
class OrderService {

    @Transactional
    Order create(Order order) {
        return orders.save(order);
    }
}
```
