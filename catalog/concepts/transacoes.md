---
id: transacoes
nome: Transações (@Transactional)
grupo: spring
aparece_em:
  - anotacao: Transactional
referencias:
  - https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
---

## O que é

Uma transação agrupa operações no banco que precisam acontecer juntas: ou todas valem, ou
nenhuma. Com `@Transactional`, o Spring abre a transação antes do método e faz commit no fim, ou
rollback se uma exceção escapar.

`readOnly = true` avisa que o método só lê, e o Hibernate economiza trabalho. Atenção: a anotação
só vale quando o método é chamado de **fora** da classe, porque é um proxy do Spring que a
aplica.

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
