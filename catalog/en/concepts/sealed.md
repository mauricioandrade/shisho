---
id: sealed
nome: Sealed types
---

## What it is

A `sealed` interface or class says exactly who may implement or extend it. The hierarchy is
closed: nobody outside adds a new case without you knowing.

The gain shows in `switch`: with a sealed hierarchy, the compiler knows every case and flags the
one you missed, with no `default` hiding what you forgot. Since Java 17 (JEP 409).

## Example

```java
sealed interface Payment permits Pix, Card {
}

record Pix(String key) implements Payment {
}

record Card(String number, int installments) implements Payment {
}
```
