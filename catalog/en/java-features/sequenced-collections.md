---
id: sequenced-collections
nome: Sequenced collections
---

## What it is

`List`, `Deque`, `LinkedHashSet` and `LinkedHashMap` gain a common interface for ordered
collections: `getFirst()`, `getLast()`, `addFirst()`, `reversed()`.

## Example

```java
Order last = orders.getLast();          // before: orders.get(orders.size() - 1)
List<Order> recent = orders.reversed();
```
