---
id: jackson-1
nome: Jackson 1.x (Codehaus)
resumo: no new release since 2013, and ignored by Spring, which uses Jackson 2 or 3
alternativa: the Jackson Spring Boot already ships (com.fasterxml.jackson in Boot 3, tools.jackson in Boot 4)
---

## Why

Jackson 1.x, from the `org.codehaus.jackson` package, had its last release (1.9.13) in 2013. Its
repository says the line is no longer actively developed. Jackson 2 moved to
`com.fasterxml.jackson` (and Jackson 3 to `tools.jackson`), and one of those is what Spring MVC
uses to convert JSON.

With both on the classpath, the annotations of one do not apply to the other: a `@JsonIgnore`
imported from `org.codehaus.jackson` is silently ignored by Spring, and the field shows up in the
JSON.
