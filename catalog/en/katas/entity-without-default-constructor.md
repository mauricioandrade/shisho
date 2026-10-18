---
id: entity-without-default-constructor
---

## Why

When Hibernate reads a row from the database, it does not know how to call the constructor you
wrote: it does not know which argument goes into which parameter. It creates the entity with the
**no-argument** constructor and fills in the fields afterwards, through reflection. The JPA
specification requires that constructor, `public` or `protected`.

A class with no constructor at all has the default one, which Java generates. The problem shows up
when someone writes a constructor with parameters, to create the entity already valid, and the
default one silently disappears. The code compiles and starts, and the first query fails with an
`InstantiationException`.

## Acceptable when

Never on an entity. The way out is a `protected` no-argument constructor next to yours: JPA accepts
it, and the rest of the code, which is neither in the same package nor a subclass, still has to use
the constructor with parameters. Lombok's `@NoArgsConstructor(access = AccessLevel.PROTECTED)` does
the same, and the kata recognizes it. A record mapped as an `@Embeddable` is left out: Hibernate 6.2
uses its canonical constructor.

## Before

```java
@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public Product(String name) {
        this.name = name;
    }
}
```

## After

```java
@Entity
public class Product {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    protected Product() {
    }

    public Product(String name) {
        this.name = name;
    }
}
```
