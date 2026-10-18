---
id: records
nome: Records
desde: 16
status: final
jep: 395
---

## O que é

Uma classe que é só os seus dados: `record Pedido(String cliente, BigDecimal total) {}` já tem
construtor, acessores, `equals`, `hashCode` e `toString`, e os campos são `final`. É o formato
natural de DTOs, respostas de API e configuração. Saiu final no Java 16, mas a LTS em que a maioria
dos projetos chega a ele é o 17, que é também o mínimo do Spring Boot 3.

## Exemplo

```java
public record CriarPedidoRequest(@NotBlank String cliente, @Positive BigDecimal total) {
}
```
