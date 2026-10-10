---
id: stream-gatherers
nome: Stream gatherers
---

## What it is

Custom intermediate operations on streams, through `Stream.gather`. The JDK already ships some in
`Gatherers`, such as fixed and sliding windows.

## Example

```java
List<List<Order>> batches = orders.stream()
        .gather(Gatherers.windowFixed(100))
        .toList();
```
