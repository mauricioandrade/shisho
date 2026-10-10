---
id: builder
nome: Builder
---

## What it is

A helper object that assembles another one step by step, with one method per field, and hands
the result over in `build()`. It is meant for objects with many fields, several of them optional,
where a constructor with eight parameters would be unreadable and easy to get in the wrong order.

With few fields, a record or a constructor does the job with less code. Lombok's `@Builder`
generates the builder by itself.

## Example

```java
class Email {

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String to;
        private String subject;

        Builder to(String to) {
            this.to = to;
            return this;
        }
    }
}
```
