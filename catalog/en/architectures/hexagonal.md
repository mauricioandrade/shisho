---
id: hexagonal
nome: Hexagonal (Ports & Adapters)
---

## What it is

The application has a **core** (domain + use cases) that knows nothing about the outside world.
It defines **ports** (interfaces): inbound, for whoever drives it, and outbound, for what it
needs (persistence, email, APIs). **Adapters** implement those ports: a REST controller, a JPA
repository, an HTTP client.

## When it is worth the cost

When the core has real business rules and the infrastructure is volatile: several external
integrations, a real chance of switching databases or vendors, a need to test the domain
without starting Spring. The outbound port allows an in-memory fake in tests.

## When it is overkill

If the domain is a CRUD with no rules, the ports protect a core that does not exist: the use
case only calls the port, the port only calls the repository, and every field is copied by
three mappers. The classic sign: interfaces with exactly one implementation that no test ever
replaces.
