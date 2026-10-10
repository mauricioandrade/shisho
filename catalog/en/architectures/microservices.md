---
id: microservices
nome: Microservices
---

## What it is

The system is split into **independently deployed services**, each one owning its domain and
its data, talking over the network.

## When it is worth the cost

It is, above all, an **organizational** solution: several teams that need to ship without
blocking each other, parts of the system with very different scaling needs. Without those
problems, the costs (network, eventual consistency, distributed observability, deployment) do
not pay off.

## When it is overkill

The most common signs: services that read the same database, a `common` module with shared
entities, services that only work if deployed together, or more services than people on the
team. That is a **distributed monolith**: the costs of microservices without the benefits. The
way back is the modular monolith.
