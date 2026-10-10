---
id: layered
nome: Technical layers (Layered)
---

## What it is

The code is grouped by **technical role**: all the controllers together, all the services
together, all the repositories together. It is the structure the Spring Initializr and most
tutorials suggest.

## When it is the right choice

For a small CRUD, an MVP or a study project, it is unbeatable: zero ceremony, and any Spring dev
understands it right away.

## Where it starts to hurt

The problem shows up as the project grows. One feature ("order") is spread over four packages,
the `service` package becomes a drawer with dozens of unrelated classes, and nothing stops
`OrderService` from calling `UserRepository` directly. Cohesion is by type, not by subject.

## The next step

When the Shishō detects signs of underengineering, the natural move is **package-by-feature**:
keeping the same layers, but inside each feature.
