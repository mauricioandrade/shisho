---
id: enum
nome: Enum
---

## What it is

A type with a fixed set of values, such as the statuses of an order. Instead of a `String` that
accepts anything (and a typo), the compiler only allows the declared values, and a `switch` warns
when a case is missing.

An enum is a real class: it can have fields, a constructor and methods, which keeps each value's
behavior next to it.

## Example

```java
enum OrderStatus {
    OPEN, PAID, SHIPPED, CANCELLED;

    boolean canCancel() {
        return this == OPEN || this == PAID;
    }
}
```
