---
id: to-one-without-lazy
referencias:
  - https://vladmihalcea.com/manytoone-jpa-hibernate/
  - https://jakarta.ee/specifications/persistence/3.1/apidocs/jakarta.persistence/jakarta/persistence/manytoone
---

## Por quê

No JPA, os relacionamentos "para muitos" são `LAZY` por padrão, e os "para um" (`@ManyToOne`,
`@OneToOne`) são `EAGER`. É uma assimetria que pega quase todo mundo: um `@ManyToOne` sem `fetch`
carrega a outra ponta em toda consulta. Uma lista de cem pedidos, cada um com o seu cliente, vira
uma consulta para os pedidos e até cem para os clientes, o N+1 que ninguém escreveu de propósito.

Com `fetch = FetchType.LAZY`, o Hibernate põe no lugar um proxy que só vai ao banco se alguém usar
o cliente. Onde o cliente é usado, um `JOIN FETCH` traz tudo numa consulta só.

## Quando é aceitável

Quando a outra ponta é usada praticamente sempre e o `EAGER` é uma escolha: escreva
`fetch = FetchType.EAGER` e a kata aceita, porque a decisão ficou visível. O lado inverso de um
`@OneToOne(mappedBy = ...)` também fica de fora: sem *bytecode enhancement*, o Hibernate não consegue
torná-lo lazy, então o `fetch` ali não muda nada.

## Antes

```java
@Entity
public class Pedido {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Cliente cliente;
}
```

## Depois

```java
@Entity
public class Pedido {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Cliente cliente;
}
```
