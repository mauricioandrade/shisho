---
id: unnamed-variables
nome: Unnamed variables and patterns
---

## What it is

`_` in place of a variable or component that is not used: it makes explicit that the value is
ignored on purpose.

## Example

```java
try {
    return Integer.parseInt(text);
} catch (NumberFormatException _) {
    return 0;
}
```
