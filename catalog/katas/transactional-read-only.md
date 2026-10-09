---
id: transactional-read-only
referencias:
  - https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html
  - https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
  - https://vladmihalcea.com/spring-read-only-transaction-hibernate-optimization/
---

## Por quê

`readOnly = true` avisa o gerenciador de transação de que nada será escrito. Com
JPA/Hibernate, o efeito é concreto: a sessão deixa de fazer flush automático e, desde o
Spring 5.1, as entidades carregadas ficam em modo somente leitura, então o Hibernate descarta a
cópia que guardaria para o dirty checking. É menos memória e menos trabalho no fim da
transação, e a diferença cresce com o número de entidades lidas.

A anotação também documenta a intenção: quem lê sabe que o método não deveria alterar nada.

## Quando é aceitável

É uma dica, não uma trava: o javadoc avisa que escritas não necessariamente falham. Se o
método lê para depois escrever (carrega a entidade e altera um campo), ele não é de leitura,
mesmo que o nome comece com `buscar`; a kata decide pelo nome e pode errar. Os métodos de
repositório do Spring Data já são `readOnly` por padrão, então o ganho aparece no service que
agrupa várias leituras numa transação.

## Antes

```java
@Service
public class PedidoService {

    @Transactional
    public List<Pedido> listarPorCliente(String cliente) {
        return pedidos.findByCliente(cliente);
    }
}
```

## Depois

Um padrão comum é marcar a classe inteira como leitura e só os métodos que escrevem com
`@Transactional`.

```java
@Service
@Transactional(readOnly = true)
public class PedidoService {

    public List<Pedido> listarPorCliente(String cliente) {
        return pedidos.findByCliente(cliente);
    }

    @Transactional
    public Pedido criar(Pedido pedido) {
        return pedidos.save(pedido);
    }
}
```
