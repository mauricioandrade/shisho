---
id: event-driven
nome: Event-Driven
---

## What it is

Components react to **events** ("order created") instead of calling each other directly. Whoever
publishes does not know who consumes. It can be internal (Spring application events) or
distributed (Kafka, RabbitMQ).

## When it is worth the cost

Work that can be asynchronous (email, notifications, slow integrations), several parties
interested in the same fact, systems from different teams that need to be decoupled.

## When it is overkill

A Kafka for a service that publishes and consumes its own events adds infrastructure, eventual
consistency and hard debugging, without decoupling anything. Inside a monolith,
`ApplicationEventPublisher` with `@TransactionalEventListener` gives the same decoupling of
code, with no broker.
