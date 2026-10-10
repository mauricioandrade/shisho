---
id: commons-lang-2
nome: Apache Commons Lang 2
resumo: replaced by Commons Lang 3, with no new release since 2011
alternativa: the standard library (Objects, String.isBlank, String.repeat) or Commons Lang 3
---

## Why

Version 2.6, from 2011, is the last one of the 2.x line. Commons Lang 3 is its successor, in a
different package (`org.apache.commons.lang3`), precisely so that both can live side by side
during the migration.

Before swapping one for the other, look at the JDK: much of what people used Commons Lang for
became standard API (`Objects.requireNonNull`, `Objects.equals`, `String.isBlank`,
`String.strip`, `String.repeat`). Sometimes the best migration is removing the dependency.
