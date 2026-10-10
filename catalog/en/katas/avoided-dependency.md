---
id: avoided-dependency
---

## Why

Every dependency is code you did not write but are responsible for. When its own project
declares it abandoned, names a successor or moves it to a new address, it stops getting fixes,
security ones included, and starts holding back upgrades of everything else: a new Spring, a new
Java, a new driver.

The Shishō only advises against a library when there is a source for it, such as the Log4j 1
end-of-life announcement, Joda-Time asking users to migrate to `java.time` or the MySQL
Connector/J release notes. Each case, with its reason and alternative, is in
`catalog/dependencies`.

## Acceptable when

During a planned migration, with both versions living together for a while. Also when another
library you depend on requires the old one and there is no way out yet; in that case, turn the
kata off in `shisho.yaml` and write down why. If the dependency only arrives transitively, the
kata does not see it: it looks at what `pom.xml` declares.

## Before

```xml
<dependencies>
    <dependency>
        <groupId>log4j</groupId>
        <artifactId>log4j</artifactId>
        <version>1.2.17</version>
    </dependency>
</dependencies>
```

## After

Spring Boot already ships SLF4J and Logback: remove Log4j 1 and use the SLF4J API.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```
