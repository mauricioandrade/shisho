---
id: onion
nome: Onion Architecture
---

## What it is

Concentric layers around the **domain model**: the model at the center, domain services,
application services and, in the outer shell, infrastructure, UI and tests. Repository
interfaces live in the core; their implementations, in the shell.

## How it relates to Clean and Hexagonal

It predates Clean Architecture and is very similar to it. The most visible difference is the
explicit split between domain services and application services.

## When it is overkill

The same signs as the rest of the family: service layers that only pass calls along and a
"domain model" that is only getters and setters.
