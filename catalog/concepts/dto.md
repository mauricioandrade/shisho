---
id: dto
nome: DTO (Data Transfer Object)
grupo: padrao
aparece_em:
  - sufixo: Request
  - sufixo: Response
  - sufixo: Dto
  - sufixo: DTO
referencias:
  - https://martinfowler.com/eaaCatalog/dataTransferObject.html
---

## O que é

Um objeto que só transporta dados de um lado para outro, sem regra de negócio: o que o cliente
envia (`Request`) e o que a API devolve (`Response`). Ele separa o contrato da API do modelo
interno: a entidade pode mudar sem quebrar quem consome a API, e campos internos não vazam.

Em Java moderno, um DTO é quase sempre um record: imutável e sem código repetido.

## Exemplo

```java
record PedidoResponse(Long id, String cliente, BigDecimal total) {

    static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), pedido.getCliente(), pedido.total());
    }
}
```
