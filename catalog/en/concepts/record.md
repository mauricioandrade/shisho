---
id: record
nome: Record
---

## What it is

A class made only to carry data. You declare the components in parentheses, and the compiler
writes the rest: constructor, accessors (`name()`, with no `get`), `equals`, `hashCode` and
`toString`. The components are `final`: once created, a record does not change.

It is the natural shape for DTOs, API responses and values that travel together, such as a
period with a start and an end. Since Java 16 (JEP 395).

## Example

```java
record Period(LocalDate start, LocalDate end) {

    boolean contains(LocalDate day) {
        return !day.isBefore(start) && !day.isAfter(end);
    }
}
```
