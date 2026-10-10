---
id: stream
nome: Streams
grupo: java
aparece_em:
  - chamada: stream
referencias:
  - https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html
---

## O que é

Uma forma de processar coleções descrevendo **o que** fazer, e não **como** percorrer: filtrar
(`filter`), transformar (`map`), juntar (`toList`, `collect`). Cada passo recebe uma função, em
geral uma lambda (`pedido -> pedido.total()`) ou uma referência de método (`Pedido::total`).

O resultado é uma nova coleção; a original não muda. Para transformações em cadeia, costuma ser
mais legível que um `for` com variáveis acumulando estado.

## Exemplo

```java
class RelatorioDeVendas {

    List<String> clientesComPedidoPago(List<Pedido> pedidos) {
        return pedidos.stream()
                .filter(Pedido::pago)
                .map(Pedido::cliente)
                .distinct()
                .toList();
    }
}
```
