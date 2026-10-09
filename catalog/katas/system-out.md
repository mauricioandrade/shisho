---
id: system-out
---

## Antes

```java
@Service
public class PedidoService {

    public void criar(Pedido pedido) {
        try {
            System.out.println("criando pedido " + pedido.id());
            salvar(pedido);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Depois

O logger tem nível (dá para silenciar o `debug` em produção), formato e destino
configuráveis, e a exceção vai inteira para o log, junto com a mensagem.

```java
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    public void criar(Pedido pedido) {
        log.debug("criando pedido {}", pedido.id());
        try {
            salvar(pedido);
        } catch (DataAccessException e) {
            log.error("falha ao salvar o pedido {}", pedido.id(), e);
            throw e;
        }
    }
}
```
