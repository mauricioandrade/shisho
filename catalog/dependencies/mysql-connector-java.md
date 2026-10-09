---
id: mysql-connector-java
nome: MySQL Connector/J com as coordenadas antigas
coordenadas:
  - mysql:mysql-connector-java
resumo: as coordenadas mudaram na versão 8.0.31, e as antigas podem deixar de ser publicadas sem aviso
alternativa: com.mysql:mysql-connector-j, a versão gerenciada pelo Spring Boot
referencias:
  - https://dev.mysql.com/doc/relnotes/connector-j/en/news-8-0-31.html
---

## Por quê

O driver é o mesmo; o que mudou foi o endereço. A partir da versão 8.0.31, o Connector/J é
publicado como `com.mysql:mysql-connector-j`, e as coordenadas antigas apontam para um POM de
realocação. As notas da versão pedem a troca "o quanto antes", porque as antigas podem ser
descontinuadas a qualquer momento.

O Spring Boot gerencia a versão das coordenadas novas: com elas, o driver acompanha o Boot sem
precisar declarar versão.
