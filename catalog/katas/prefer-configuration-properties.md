---
id: prefer-configuration-properties
referencias:
  - https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties
  - https://docs.spring.io/spring-boot/reference/features/external-config.html#features.external-config.typesafe-configuration-properties.vs-value-annotation
---

## Por quê

Várias propriedades com o mesmo prefixo são um conceito só (a configuração do e-mail, de um
cliente HTTP) espalhado em strings soltas. Um erro de digitação em `${loja.email.host}` só
aparece em tempo de execução, e cada classe que precisa da configuração repete as chaves.

Com `@ConfigurationProperties`, o grupo vira um tipo: as chaves são declaradas uma vez, os
valores são convertidos (`Duration`, `DataSize`, listas) e validados na inicialização, e, com o
`spring-boot-configuration-processor`, a IDE autocompleta o `application.yml`. A documentação
do Spring Boot recomenda esse caminho para as chaves dos seus próprios componentes.

## Quando é aceitável

Com duas ou três propriedades usadas num único lugar, `@Value` no construtor é simples e
suficiente (a kata só aparece a partir de três com o mesmo prefixo). E `@Value` é a única opção
para expressões SpEL.

## Antes

```java
@Service
public class EmailService {

    public EmailService(
            @Value("${loja.email.host}") String host,
            @Value("${loja.email.porta}") int porta,
            @Value("${loja.email.remetente}") String remetente) {
        this.cliente = new ClienteSmtp(host, porta, remetente);
    }
}
```

## Depois

```java
@ConfigurationProperties(prefix = "loja.email")
public record EmailProperties(String host, int porta, String remetente) {
}

@Service
public class EmailService {

    public EmailService(EmailProperties email) {
        this.cliente = new ClienteSmtp(email.host(), email.porta(), email.remetente());
    }
}
```
