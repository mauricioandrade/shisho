---
id: joda-time
nome: Joda-Time
coordenadas:
  - joda-time:joda-time
resumo: substituído pelo java.time do próprio JDK, e o projeto pede que se migre
alternativa: java.time (LocalDate, Instant, Duration...)
referencias:
  - https://www.joda.org/joda-time/
  - https://openjdk.org/jeps/150
---

## Por quê

O Joda-Time foi o padrão de fato para datas em Java até o Java 8, e inspirou o `java.time`
(JSR-310), criado pelo mesmo autor. A página do projeto pede: "users are now asked to migrate to
`java.time`". Ele ainda recebe atualizações de fuso horário, mas não evolui.

O `java.time` está no JDK, é imutável e é suportado nativamente pelo Jackson, pelo JPA e pelo
Spring. Uma dependência a menos, e a mesma API que o resto do ecossistema usa.
