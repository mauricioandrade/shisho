---
id: virtual-threads
nome: Virtual threads
---

## What it is

Lightweight threads, managed by the JVM rather than the operating system. Blocking a virtual
thread (waiting on HTTP, a database, a file) is cheap: you can have one per task, hundreds of
thousands of them, with no pool. In Spring Boot 3.2+, `spring.threads.virtual.enabled=true`
turns them on for the web server.

## Example

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    orders.forEach(order -> executor.submit(() -> notify(order)));
}
```
