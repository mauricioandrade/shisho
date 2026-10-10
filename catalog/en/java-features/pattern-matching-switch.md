---
id: pattern-matching-switch
nome: Pattern matching for switch
---

## What it is

`switch` over types, with guards (`when`) and exhaustiveness checking when the hierarchy is
`sealed`: forgetting a case becomes a compile error, not a silent `else`.

## Example

```java
String description = switch (payment) {
    case Pix pix -> "Pix " + pix.key();
    case Card card when card.installments() > 1 -> card.installments() + " card installments";
    case Card _ -> "card, single payment";
};
```
