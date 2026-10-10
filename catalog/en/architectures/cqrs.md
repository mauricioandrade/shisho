---
id: cqrs
nome: CQRS (Command Query Responsibility Segregation)
---

## What it is

Separating the model that **changes** state (commands) from the model that **reads** state
(queries). In extreme cases each side has its own database, and the read side is updated by
events.

## When it is worth the cost

When reads and writes have very different shapes and loads: screens that aggregate data from
several places, heavy reports, read volume orders of magnitude larger. And almost always **in
one part** of the system, not all of it.

## When it is overkill

If the read model is the same as the write model and both use the same tables, the project only
doubled its classes. A light version solves most cases: a query method with a projection (a
record) and `@Transactional(readOnly = true)`, with no bus at all.
