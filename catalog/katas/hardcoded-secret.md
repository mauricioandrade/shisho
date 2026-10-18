---
id: hardcoded-secret
referencias:
  - https://owasp.org/Top10/A07_2021-Identification_and_Authentication_Failures/
  - https://12factor.net/config
  - https://docs.spring.io/spring-boot/reference/features/external-config.html
  - https://quarkus.io/guides/config-reference#environment-variables
---

## Por quê

Uma senha no `application.properties` vai para o repositório junto com o código. Quem tem acesso
ao repositório (o time, um ex-funcionário, um fornecedor, um vazamento) tem acesso ao banco. E o
Git não esquece: apagar a linha num commit novo não tira a senha do histórico.

O segredo também fica preso ao código. Trocá-lo, depois de um vazamento ou por rotina, pede um
commit e um deploy. E o mesmo valor acaba servindo a todos os ambientes, porque é um só arquivo.

Spring e Quarkus resolvem isso do mesmo jeito: o arquivo diz **de onde** vem o valor
(`${DB_PASSWORD}`), e o ambiente diz **qual** é. Uma variável de ambiente, um Secret do
Kubernetes, um cofre como o Vault ou o AWS Secrets Manager.

## Quando é aceitável

Num perfil de desenvolvimento (`application-dev.properties`, `%dev.` no Quarkus), com a senha do
banco local que sobe no `docker compose`. O Shishō não acusa esses perfis. Também não acusa um
valor vazio nem um placeholder, mesmo com padrão (`${DB_PASSWORD:local}`), mas o padrão também é
lido do repositório: guarde ali só o que serve para a sua máquina.

A lição nunca mostra o valor, para não espalhar o segredo pelo relatório e pelo log do CI.

## Antes

```properties
spring.datasource.url=jdbc:postgresql://db:5432/loja
spring.datasource.password=s3nh4-d0-banc0
jwt.secret=nRvyYC4soFxBdZ
```

## Depois

O valor vem do ambiente: `DB_PASSWORD` e `JWT_SECRET`, definidos no deploy, num Secret do
Kubernetes ou no `.env` que não vai para o Git.

```properties
spring.datasource.url=jdbc:postgresql://db:5432/loja
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
```
