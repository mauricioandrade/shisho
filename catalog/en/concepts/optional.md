---
id: optional
nome: Optional
---

## What it is

A box that either holds a value or is empty. A method returning `Optional<Order>` says, in its own
signature, that the order may not exist, and makes the caller decide what to do: throw an error
(`orElseThrow`), use a default (`orElse`) or transform the value (`map`).

It replaces `null` as the "not found" return, the most common cause of `NullPointerException`. It
was not made for fields or parameters: only for return values.

## Example

```java
class OrderLookup {

    Order find(Long id) {
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }
}
```
