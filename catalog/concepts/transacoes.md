---
id: transacoes
nome: Transações (@Transactional)
grupo: jakarta
ordem: 20
aparece_em:
  - anotacao: Transactional
referencias:
  - https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
  - https://quarkus.io/guides/transaction
---

## O que é

Uma transação agrupa operações no banco que precisam acontecer juntas: ou todas valem, ou
nenhuma. Com `@Transactional`, o framework abre a transação antes do método e faz commit no fim,
ou rollback se uma exceção não verificada escapar.

No Spring, `readOnly = true` avisa que o método só lê, e o Hibernate economiza trabalho; o
`@Transactional` do Jakarta, que o Quarkus usa, não tem essa opção. Nos dois, a anotação só vale
quando o método é chamado de fora da classe, porque quem a aplica é um proxy que envolve o bean.

## Exemplo

```java
@Service
@Transactional(readOnly = true)
class PedidoService {

    @Transactional
    Pedido criar(Pedido pedido) {
        return pedidos.save(pedido);
    }
}
```
