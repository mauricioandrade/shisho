---
id: transactional-controller
referencias:
  - https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
  - https://martinfowler.com/eaaCatalog/serviceLayer.html
---

## Por quê

A transação existe para proteger uma regra de negócio: "debitar e creditar, ou nenhum dos dois". O
lugar dessa regra é o service, e é nele que a fronteira da transação faz sentido. Quando o
`@Transactional` vai para o controller, duas coisas acontecem.

A primeira é de desenho: se a transação está no controller, a regra que ela protege também está, e
o controller deixa de só traduzir HTTP. A segunda é prática: a transação (e a conexão do banco que
ela segura) passa a durar o tratamento inteiro da requisição, da leitura do corpo à montagem da
resposta, e não só o trabalho no banco.

## Quando é aceitável

Num CRUD sem camada de service, em que o controller chama o repositório direto e não há regra a
proteger. É o que o JHipster gera quando se escolhe não ter service. Se a aplicação crescer, o
primeiro passo é criar o service e levar a transação para lá. No Quarkus a kata não roda: lá, um
resource com `@Transactional` é o jeito que os próprios guias ensinam.

## Antes

```java
@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {

    @PostMapping
    @Transactional
    public void transferir(@Valid @RequestBody TransferenciaRequest request) {
        contas.debitar(request.origem(), request.valor());
        contas.creditar(request.destino(), request.valor());
    }
}
```

## Depois

```java
@RestController
@RequestMapping("/transferencias")
public class TransferenciaController {

    @PostMapping
    public void transferir(@Valid @RequestBody TransferenciaRequest request) {
        transferencias.transferir(request.origem(), request.destino(), request.valor());
    }
}
```
