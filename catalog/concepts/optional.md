---
id: optional
nome: Optional
grupo: java
ordem: 40
aparece_em:
  - import: java.util.Optional
  - chamada: orElseThrow
referencias:
  - https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html
---

## O que é

Uma caixa que tem um valor ou está vazia. Um método que devolve `Optional<Pedido>` avisa, na
própria assinatura, que o pedido pode não existir, e obriga quem chama a decidir o que fazer:
lançar um erro (`orElseThrow`), usar um padrão (`orElse`) ou transformar o valor (`map`).

É o substituto do `null` como retorno de "não encontrei", a causa mais comum de
`NullPointerException`. Não foi feito para campos nem parâmetros: só para retornos.

## Exemplo

```java
class BuscaDePedido {

    Pedido buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new PedidoInexistenteException(id));
    }
}
```
