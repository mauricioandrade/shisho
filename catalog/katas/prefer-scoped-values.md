---
id: prefer-scoped-values
referencias:
  - https://openjdk.org/jeps/506
  - https://openjdk.org/jeps/444
---

## Por quê

Um `ThreadLocal` é um parâmetro escondido: qualquer código na mesma thread lê e altera o
valor, ele vive enquanto a thread viver e cabe a você chamar `remove()`. Num pool de threads,
esquecer o `remove()` vaza o contexto de uma requisição para a próxima, o que vira falha de
segurança quando o valor é o usuário logado.

`ScopedValue` (JEP 506, final no Java 25) é feito para o caso comum: passar um dado imutável
para baixo na pilha de chamadas. O valor vale só dentro do bloco `where(...).run(...)`, não pode
ser trocado no meio e some sozinho ao sair. Com milhares de virtual threads, também evita uma
cópia do valor por thread.

## Quando é aceitável

A JEP 506 não deprecia o `ThreadLocal`. Ele continua certo para cache por thread de objetos
mutáveis e caros (o exemplo clássico é o `SimpleDateFormat`, hoje substituível pelo
`DateTimeFormatter`) e para dados que sobem e descem na pilha. Bibliotecas que você não controla,
como o MDC do SLF4J e o `SecurityContextHolder` do Spring Security, ainda usam `ThreadLocal`, e
não há o que mudar nelas.

## Antes

```java
public final class ContextoDaRequisicao {

    private static final ThreadLocal<Usuario> USUARIO = new ThreadLocal<>();

    public static void executarComo(Usuario usuario, Runnable tarefa) {
        USUARIO.set(usuario);
        try {
            tarefa.run();
        } finally {
            USUARIO.remove();
        }
    }
}
```

## Depois

O valor vale só dentro do `run` e não pode ser trocado no meio do caminho; não há `remove()`
para esquecer.

```java
public final class ContextoDaRequisicao {

    private static final ScopedValue<Usuario> USUARIO = ScopedValue.newInstance();

    public static void executarComo(Usuario usuario, Runnable tarefa) {
        ScopedValue.where(USUARIO, usuario).run(tarefa);
    }
}
```
