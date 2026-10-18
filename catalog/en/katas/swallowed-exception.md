---
id: swallowed-exception
---

## Why

An empty `catch` turns an error into silence. The program goes on as if nothing had happened, with
a state that may be half done, and the symptom shows up later, far from its cause: a null field, a
file that was never written, an order that vanished. Whoever investigates has no log, stack trace
or message to start from.

There are three honest ways out. **Log** it, when the program can go on without the result.
**Rethrow** it, wrapped in an exception that says what failed (`throw new UnreadableFileException(path, e)`),
when the caller needs to know. And **ignore it on purpose**, saying so in the code: a comment with the
reason, or a variable named `ignored` (or `_`, unnamed, from Java 22), shows the reader that the
silence was a decision.

## Acceptable when

When the exception is expected and changes nothing, such as a `close()` that fails after the work
is done, or a duplicate key in an idempotent operation. In those cases, write why in a comment
inside the `catch`, or name the variable `ignored`: the kata accepts both, and the reader
understands. What it flags is the mute block, with no statement and no explanation.

## Before

```java
@Service
public class ReportService {

    public String load(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
        }
        return "";
    }
}
```

## After

```java
@Service
public class ReportService {

    public String load(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UnreadableReportException(file, e);
        }
    }
}
```
