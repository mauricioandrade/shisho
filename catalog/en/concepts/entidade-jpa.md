---
id: entidade-jpa
nome: JPA entity
---

## What it is

A class that represents a database table: each object is a row, each field a column. `@Id` marks
the primary key, and JPA (with Hibernate underneath) writes the SQL to find, insert and update.

Inside a transaction, the entity is managed: Hibernate notices what changed in it and writes it
by itself at the end, without you calling `save`. That is why it should not go straight to the
API: the JSON contract would be tied to the database schema.

## Example

```java
@Entity
class Order {

    @Id
    @GeneratedValue
    private Long id;
    private String customer;

    protected Order() {
    }
}
```
