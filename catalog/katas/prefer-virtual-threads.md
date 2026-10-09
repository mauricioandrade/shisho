---
id: prefer-virtual-threads
referencias:
  - https://openjdk.org/jeps/444
  - https://openjdk.org/jeps/491
  - https://docs.spring.io/spring-boot/reference/features/spring-application.html
---

## Por quê

Uma thread de plataforma é uma thread do sistema operacional: custa memória e troca de
contexto, então é preciso limitá-las num pool. Para tarefas que passam a maior parte do tempo
esperando (HTTP, banco, arquivo), o tamanho do pool vira o limite de vazão, e dimensioná-lo é
adivinhação.

Virtual threads (JEP 444, final no Java 21) são gerenciadas pela JVM: quando bloqueiam, liberam
a thread do sistema. São baratas a ponto de uma por tarefa, e a JEP diz que nunca é preciso
fazer pool delas. Não são mais rápidas, só escalam: aumentam a vazão, não reduzem a latência.

## Quando é aceitável

Para trabalho de CPU (cálculo, compressão, criptografia), mais threads que núcleos não ajuda:
o pool fixo do tamanho dos núcleos continua certo. Se o pool servia para limitar a concorrência
(no máximo 10 chamadas a uma API externa), use um `Semaphore` com virtual threads. Do Java 21 ao
23, um bloco `synchronized` que espera I/O prende a thread do sistema (pinning); o Java 24
resolveu isso (JEP 491), e o Spring Boot recomenda Java 24 ou superior para virtual threads.

## Antes

```java
@Service
public class NotificacaoService {

    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    public void notificarTodos(List<Pedido> pedidos) {
        pedidos.forEach(pedido -> executor.submit(() -> enviarEmail(pedido)));
    }
}
```

## Depois

Enviar e-mail é esperar a rede: com virtual threads, cada envio ganha a sua, sem dimensionar
pool. Para cálculo pesado (CPU), o pool fixo do tamanho dos núcleos continua o certo.

```java
@Service
public class NotificacaoService {

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void notificarTodos(List<Pedido> pedidos) {
        pedidos.forEach(pedido -> executor.submit(() -> enviarEmail(pedido)));
    }
}
```
