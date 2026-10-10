---
id: clean
nome: Clean Architecture
---

## What it is

Concentric circles with a single rule: **dependencies point inward**. Entities at the center,
use cases around them, interface adapters and frameworks at the edge. The domain does not know
that Spring, JPA or HTTP exist.

## The practical difference from Hexagonal

The goals are the same (an isolated core, pluggable infrastructure). Clean is more prescriptive
about the layers and tends to produce more classes; Hexagonal only talks about ports and
adapters. The Shishō tells them apart by their packages, but treats both as the same family.

## When it is overkill

It is the architecture most often cited in portfolio projects, and the one that most often
shows up as overengineering. For every CRUD endpoint: a request, a command, a use case
(interface + implementation), a domain entity, a JPA entity, two mappers, a response. If the
domain has no invariants to protect, all that machinery only copies fields from one side to the
other.
