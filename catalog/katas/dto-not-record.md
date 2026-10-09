---
id: dto-not-record
---

## Antes

```java
public class CriarPedidoRequest {

    private String cliente;
    private int quantidade;

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }
}
```

## Depois

O Jackson e o Bean Validation funcionam com records: as anotações vão nos componentes.

```java
public record CriarPedidoRequest(@NotBlank String cliente, @Positive int quantidade) {
}
```
