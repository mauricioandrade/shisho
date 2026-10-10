---
id: modular-monolith
nome: Modular Monolith
---

## What it is

A single deployment, but split into **modules with verified boundaries**: each module exposes a
small public API and hides the rest. Modules talk through that API or through application
events. In the Spring ecosystem, Spring Modulith checks these rules in a test.

## Why it is the "healthy middle ground"

It delivers almost every organizational benefit people look for in microservices (clear
boundaries, independent teams in the code) with no network, no eventual consistency and no
distributed infrastructure. If a module ever needs to move out, the boundary is already there.

## When it is overkill

With two or three small modules and a single dev, the formality (events, `package-info`,
verification) costs more than package-by-feature, which already does the job.
