---
id: missing-actuator
referencias:
  - https://docs.spring.io/spring-boot/reference/actuator/endpoints.html
---

## Por quê

Em produção, alguém precisa saber se a aplicação está viva e pronta para receber tráfego: o
Kubernetes (probes de liveness e readiness), um load balancer, o monitoramento. O actuator dá
isso pronto em `/actuator/health`, checando inclusive o banco e outras dependências, e publica
métricas via Micrometer.

Por padrão, só o `health` fica exposto na web. Endpoints como `env`, `configprops` e `heapdump`
revelam informação sensível; a documentação do Spring Boot recomenda expor só o necessário e
protegê-los, com Spring Security ou isolamento de rede.

## Quando é aceitável

Em aplicações que não rodam como serviço (um batch que termina, uma ferramenta local) ou
quando a plataforma já monitora de outro jeito. O starter também aumenta a superfície exposta,
então adicioná-lo é uma troca consciente, não uma escolha neutra.

## Antes

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

## Depois

Por padrão só `/actuator/health` fica exposto na web; exponha outros endpoints com
`management.endpoints.web.exposure.include`.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```
