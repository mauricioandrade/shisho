---
id: eager-collection
referencias:
  - https://vladmihalcea.com/eager-fetching-is-a-code-smell/
  - https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html
---

## Por quê

`fetch = FetchType.EAGER` numa coleção diz ao Hibernate: sempre que carregar esta entidade, carregue
a coleção junto. "Sempre" inclui a tela que só mostra o nome do cliente, o relatório que conta
pedidos e o endpoint que devolve uma lista de cem clientes. Numa lista, o Hibernate faz uma consulta
para os clientes e mais uma para os pedidos de cada um (o famoso N+1), ou junta tudo num JOIN que
multiplica as linhas.

O problema é que a decisão fica no lugar errado. Se a coleção é ou não necessária depende do caso
de uso, e o mapeamento é um só para todos. Deixe a coleção `LAZY`, que já é o padrão para
`@OneToMany` e `@ManyToMany`, e busque-a só onde ela é usada: com `JOIN FETCH` na consulta ou com
`@EntityGraph` no repositório.

## Quando é aceitável

Numa coleção pequena e sempre usada junto com a entidade, como os itens de um pedido que nunca é
mostrado sem eles. Mesmo assim, um `@EntityGraph` no método do repositório deixa a decisão
explícita, e a entidade não obriga os outros usos a carregar o que não precisam.

## Antes

```java
@Entity
public class Cliente {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "cliente", fetch = FetchType.EAGER)
    private List<Pedido> pedidos = new ArrayList<>();
}
```

## Depois

```java
@Entity
public class Cliente {

    @Id
    @GeneratedValue
    private Long id;

    @OneToMany(mappedBy = "cliente")
    private List<Pedido> pedidos = new ArrayList<>();
}
```
