---
id: joda-time
nome: Joda-Time
resumo: replaced by the JDK's own java.time, and the project asks users to migrate
alternativa: java.time (LocalDate, Instant, Duration...)
---

## Why

Joda-Time was the de facto standard for dates in Java until Java 8, and it inspired `java.time`
(JSR-310), created by the same author. The project page says: "users are now asked to migrate to
`java.time`". It still gets time zone updates, but it no longer evolves.

`java.time` is in the JDK, is immutable and is supported natively by Jackson, JPA and Spring.
One dependency less, and the same API the rest of the ecosystem uses.
