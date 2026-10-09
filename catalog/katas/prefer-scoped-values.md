---
id: prefer-scoped-values
---

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
