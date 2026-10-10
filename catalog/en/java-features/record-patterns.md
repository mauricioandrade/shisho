---
id: record-patterns
nome: Record patterns
---

## What it is

Takes a record apart right in the `instanceof` or the `switch`, without calling the accessors one
by one.

## Example

```java
if (event instanceof OrderCreated(var id, var customer)) {
    notify(customer, id);
}
```
