---
id: structured-concurrency
nome: Structured concurrency
---

## What it is

Treats a group of concurrent tasks as a unit: if one fails, the others are cancelled, and none
outlives the block that created them. Still in preview in Java 25: the Shishō never suggests
preview features.

## Example

```java
try (var scope = StructuredTaskScope.open()) {
    var customer = scope.fork(() -> findCustomer(id));
    var orders = scope.fork(() -> findOrders(id));
    scope.join();
    return new Summary(customer.get(), orders.get());
}
```
