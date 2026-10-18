---
id: swallowed-exception
referencias:
  - https://wiki.sei.cmu.edu/confluence/display/java/ERR00-J.+Do+not+suppress+or+ignore+checked+exceptions
  - https://openjdk.org/jeps/456
---

## Por quê

Um `catch` vazio transforma um erro em silêncio. O programa segue como se nada tivesse acontecido,
com um estado que talvez esteja pela metade, e o sintoma aparece depois, longe da causa: um campo
nulo, um arquivo que não foi gravado, um pedido que sumiu. Quem investiga não tem log, stack trace
nem mensagem para começar.

Há três saídas honestas. **Registrar** num logger, quando dá para seguir sem o resultado.
**Relançar**, envolvendo numa exceção que diga o que falhou (`throw new ArquivoIlegivelException(caminho, e)`),
quando quem chamou precisa saber. E **ignorar de propósito**, dizendo isso no código: um comentário com o
porquê, ou uma variável chamada `ignored` (ou `_`, sem nome, a partir do Java 22), mostra a quem
lê que o silêncio foi uma decisão.

## Quando é aceitável

Quando a exceção é esperada e não muda nada, como um `close()` que falha depois que o trabalho já
terminou, ou uma chave duplicada numa operação idempotente. Nesses casos, escreva o porquê num
comentário dentro do `catch`, ou chame a variável de `ignored`: a kata aceita os dois, e quem lê
entende. O que ela acusa é o bloco mudo, sem instrução e sem explicação.

## Antes

```java
@Service
public class RelatorioService {

    public String carregar(Path arquivo) {
        try {
            return Files.readString(arquivo);
        } catch (IOException e) {
        }
        return "";
    }
}
```

## Depois

```java
@Service
public class RelatorioService {

    public String carregar(Path arquivo) {
        try {
            return Files.readString(arquivo);
        } catch (IOException e) {
            throw new RelatorioIlegivelException(arquivo, e);
        }
    }
}
```
