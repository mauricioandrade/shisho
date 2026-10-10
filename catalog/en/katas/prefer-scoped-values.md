---
id: prefer-scoped-values
---

## Why

A `ThreadLocal` is a hidden parameter: any code on the same thread reads and changes the value,
it lives as long as the thread lives and calling `remove()` is up to you. In a thread pool,
forgetting `remove()` leaks one request's context into the next, which becomes a security flaw
when the value is the logged-in user.

`ScopedValue` (JEP 506, final in Java 25) is made for the common case: passing an immutable piece
of data down the call stack. The value only holds inside the `where(...).run(...)` block, cannot
be swapped halfway and disappears by itself on the way out. With thousands of virtual threads, it
also avoids one copy of the value per thread.

## Acceptable when

JEP 506 does not deprecate `ThreadLocal`. It is still right for a per-thread cache of mutable,
expensive objects (the classic example is `SimpleDateFormat`, which `DateTimeFormatter` replaces
today) and for data that travels both up and down the stack. Libraries you do not control, such
as SLF4J's MDC and Spring Security's `SecurityContextHolder`, still use `ThreadLocal`, and there
is nothing to change in them.

## Before

```java
public final class RequestContext {

    private static final ThreadLocal<User> USER = new ThreadLocal<>();

    public static void runAs(User user, Runnable task) {
        USER.set(user);
        try {
            task.run();
        } finally {
            USER.remove();
        }
    }
}
```

## After

The value only holds inside `run` and cannot be swapped halfway; there is no `remove()` to
forget.

```java
public final class RequestContext {

    private static final ScopedValue<User> USER = ScopedValue.newInstance();

    public static void runAs(User user, Runnable task) {
        ScopedValue.where(USER, user).run(task);
    }
}
```
