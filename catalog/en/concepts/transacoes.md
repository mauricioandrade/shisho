---
id: transacoes
nome: Transactions (@Transactional)
---

## What it is

A transaction groups database operations that must happen together: either all of them count, or
none does. With `@Transactional`, Spring opens the transaction before the method and commits at
the end, or rolls back if an exception escapes.

`readOnly = true` says the method only reads, and Hibernate saves work. Watch out: the annotation
only applies when the method is called from outside the class, because a Spring proxy is what
applies it.

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
