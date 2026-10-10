---
id: stream
nome: Streams
---

## What it is

A way to process collections by describing what to do, not how to loop: filter (`filter`),
transform (`map`), collect (`toList`, `collect`). Each step takes a function, usually a lambda
(`order -> order.total()`) or a method reference (`Order::total`).

The result is a new collection; the original does not change. For chained transformations, it
usually reads better than a `for` loop with variables accumulating state.

## Example

```java
class SalesReport {

    List<String> customersWithPaidOrders(List<Order> orders) {
        return orders.stream()
                .filter(Order::paid)
                .map(Order::customer)
                .distinct()
                .toList();
    }
}
```
