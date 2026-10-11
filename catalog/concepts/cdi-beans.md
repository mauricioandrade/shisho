---
id: cdi-beans
nome: Beans CDI e escopos
grupo: quarkus
ordem: 10
aparece_em:
  - anotacao: ApplicationScoped
  - anotacao: Singleton
  - anotacao: RequestScoped
  - anotacao: Dependent
referencias:
  - https://quarkus.io/guides/cdi
  - https://quarkus.io/guides/cdi-reference
---

## O que é

No Quarkus, os beans são do CDI (Jakarta Contexts and Dependency Injection), implementado pelo
ArC. A anotação de escopo diz quanto tempo o objeto vive: `@ApplicationScoped`, um por aplicação,
criado no primeiro uso; `@RequestScoped`, um por requisição; `@Singleton`, um por aplicação, sem
proxy; `@Dependent`, um novo para cada lugar que o injeta.

Quem precisa do bean o recebe com `@Inject`, num campo package-private ou no construtor. A
diferença para o Spring é o momento: o ArC descobre os beans e monta a injeção na compilação, e
não quando a aplicação sobe.

## Exemplo

```java
@ApplicationScoped
class CalculoDeFrete {

    @Inject
    TabelaDePrecos precos;

    BigDecimal calcular(Pedido pedido) {
        return pedido.peso().multiply(precos.porQuilo());
    }
}
```
