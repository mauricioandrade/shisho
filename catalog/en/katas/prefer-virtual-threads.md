---
id: prefer-virtual-threads
---

## Why

A platform thread is an operating system thread: it costs memory and context switches, so it has
to be limited in a pool. For tasks that spend most of their time waiting (HTTP, a database, a
file), the pool size becomes the throughput limit, and sizing it is guesswork.

Virtual threads (JEP 444, final in Java 21) are managed by the JVM: when they block, they release
the operating system thread. They are cheap enough to have one per task, and the JEP says they
never need pooling. They are not faster, they just scale: they raise throughput, not lower
latency.

## Acceptable when

For CPU work (calculations, compression, cryptography), more threads than cores does not help: a
fixed pool the size of the cores is still right. If the pool was there to limit concurrency (at
most 10 calls to an external API), use a `Semaphore` with virtual threads. From Java 21 to 23, a
`synchronized` block waiting on I/O holds the operating system thread (pinning); Java 24 fixed
that (JEP 491), and Spring Boot recommends Java 24 or later for virtual threads.

## Before

```java
@Service
public class NotificationService {

    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    public void notifyAll(List<Order> orders) {
        orders.forEach(order -> executor.submit(() -> sendEmail(order)));
    }
}
```

## After

Sending an email is waiting on the network: with virtual threads, each send gets its own, with no
pool to size. For heavy calculation (CPU), a fixed pool the size of the cores is still the right
choice.

```java
@Service
public class NotificationService {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void notifyAll(List<Order> orders) {
        orders.forEach(order -> executor.submit(() -> sendEmail(order)));
    }
}
```
