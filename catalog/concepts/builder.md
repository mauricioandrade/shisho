---
id: builder
nome: Builder
grupo: padrao
ordem: 40
aparece_em:
  - detector: builder
  - anotacao: Builder
referencias:
  - https://refactoring.guru/design-patterns/builder
---

## O que é

Um objeto auxiliar que monta outro passo a passo, com um método por campo, e entrega o resultado
no `build()`. Serve para objetos com muitos campos, vários opcionais, em que um construtor com
oito parâmetros seria ilegível e fácil de errar na ordem.

Com poucos campos, um record ou um construtor resolvem com menos código. O `@Builder` do Lombok
gera o builder sozinho.

## Exemplo

```java
class Email {

    static Builder builder() {
        return new Builder();
    }

    static class Builder {
        private String para;
        private String assunto;

        Builder para(String para) {
            this.para = para;
            return this;
        }
    }
}
```
