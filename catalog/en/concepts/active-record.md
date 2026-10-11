---
id: active-record
nome: Active Record
---

## What it is

An object that is at the same time the table row and the code that saves it: `order.persist()`,
`Order.findById(id)`. There is no separate repository; the class itself knows how to save and find
itself.

It is the opposite of Repository. It is shorter and more direct when the entity is mostly data, as
in a CRUD. When the domain gains rules, mixing rules and database in the same class starts to
weigh, and splitting out a repository becomes worth it again.

## Example

```java
@Entity
public class Order extends PanacheEntity {

    public String customer;

    public void cancel() {
        delete();
    }
}
```
