---
id: system-out
referencias:
  - https://docs.spring.io/spring-boot/reference/features/logging.html
  - https://openjdk.org/jeps/264
  - https://www.slf4j.org/manual.html
  - https://quarkus.io/guides/logging
---

## Por quê

`System.out` não tem nível: não dá para calar o detalhe em produção nem ligar o `debug` só de
um pacote ao investigar um problema. Não tem formato: sai sem horário, thread ou classe de
origem, e sem a saída estruturada (JSON) que um agregador de logs indexa. E escreve sempre no
mesmo lugar.

`printStackTrace()` é pior. A pilha vai para `System.err`, separada da mensagem e fora do log,
e costuma estar num `catch` que engole a exceção e deixa o sistema seguir em estado
inconsistente.

O Spring Boot já vem com SLF4J e Logback configurados. Fora dele, o próprio JDK tem
`System.Logger` desde o Java 9 (JEP 264), uma fachada que encaminha para o framework de log
presente.

No Quarkus, o atalho é o `io.quarkus.logging.Log`: `Log.info("Pedido criado")` direto, sem
declarar um logger por classe. Na compilação, o Quarkus troca cada chamada por um logger da classe
que a fez, então o log continua dizendo de onde veio.

## Quando é aceitável

Quando o console é a interface: o `main` de uma ferramenta de linha de comando (o próprio
Shishō escreve o relatório no console), scripts e exemplos didáticos. Nesses casos a saída é o
produto, não diagnóstico. Numa aplicação de servidor, não.

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

## Antes (Quarkus)

```java
@ApplicationScoped
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

## Depois (Quarkus)

`Log`, de `io.quarkus.logging`, dispensa declarar o logger: na compilação, o Quarkus troca cada
chamada por um logger da própria classe. Nível, formato e destino continuam configuráveis em
`quarkus.log.*`.

```java
@ApplicationScoped
public class PedidoService {

    public void criar(Pedido pedido) {
        Log.debugf("criando pedido %s", pedido.id());
        try {
            salvar(pedido);
        } catch (PersistenceException e) {
            Log.errorf(e, "falha ao salvar o pedido %s", pedido.id());
            throw e;
        }
    }
}
```
