---
id: package-by-feature
nome: Package by Feature
---

## What it is

The first level of packages is the **subject** (`user`, `order`, `payment`); inside each one,
the technical classes of that feature. The same simplicity as layered, with much more cohesion.

## Why it is usually the best starting point

Everything that changes together stays together. Deleting a feature means deleting a package.
Classes can be package-private, exposing only the service, which already creates boundaries
without any framework.

## Where it starts to hurt

When features start reading each other's entities and cycles appear, the boundaries have become
decorative. That is the sign to formalize them with a **modular monolith**.

## How it relates to Clean/Hexagonal

They are not mutually exclusive: you can have `order/domain`, `order/application`,
`order/infrastructure`. The Shishō reports that combination as "package-by-feature +
hexagonal".
