---
id: observer
nome: Observer (events)
---

## What it is

Whoever does something publishes an event ("order paid"), and whoever cares reacts to it, without
either knowing the other. The order service does not need to know that email, invoicing or stock
exist: each one listens to the event it cares about.

In Spring, `publishEvent` publishes and `@EventListener` listens. With
`@TransactionalEventListener`, the reaction only runs after the commit, so no receipt goes out for
an order that was never saved.

## Example

```java
record OrderPaid(Long orderId) {
}

@Component
class ReceiptSender {

    @TransactionalEventListener
    void whenPaid(OrderPaid event) {
        sendReceipt(event.orderId());
    }
}
```
