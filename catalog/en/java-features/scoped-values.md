---
id: scoped-values
nome: Scoped values
---

## What it is

An immutable value shared with everything a piece of code calls, including on other threads,
which disappears when that piece of code ends. It replaces `ThreadLocal` for request context: it
does not leak between tasks, needs no `remove()` and is cheap with virtual threads.

## Example

```java
private static final ScopedValue<User> CURRENT_USER = ScopedValue.newInstance();

ScopedValue.where(CURRENT_USER, user).run(() -> process(order));
```
