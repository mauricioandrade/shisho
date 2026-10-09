---
id: avoided-dependency
referencias:
  - https://logging.apache.org/log4j/1.x/
  - https://www.joda.org/joda-time/
  - https://dev.mysql.com/doc/relnotes/connector-j/en/news-8-0-31.html
---

## Por quê

Toda dependência é código que você não escreveu, mas pelo qual responde. Quando o próprio
projeto declara que a abandonou, que existe um sucessor ou que mudou de endereço, ela para de
receber correções, inclusive de segurança, e passa a travar a atualização do resto: um Spring
novo, um Java novo, um driver novo.

O Shishō só desaconselha uma biblioteca quando há fonte para isso, como o anúncio de fim de vida
do Log4j 1, o pedido do Joda-Time para migrar ao `java.time` ou as notas de versão do MySQL
Connector/J. Cada caso, com o motivo e a alternativa, está em `catalog/dependencies`.

## Quando é aceitável

Durante uma migração planejada, com as duas versões convivendo por um tempo. Também quando outra
biblioteca de que você depende exige a antiga e ainda não há saída; nesse caso, desligue a kata
no `shisho.yaml` e registre o motivo. Se a dependência só chega de forma transitiva, a kata não a
vê: ela olha o que o `pom.xml` declara.

## Antes

```xml
<dependencies>
    <dependency>
        <groupId>log4j</groupId>
        <artifactId>log4j</artifactId>
        <version>1.2.17</version>
    </dependency>
</dependencies>
```

## Depois

O Spring Boot já traz SLF4J e Logback: basta remover o Log4j 1 e usar a API do SLF4J.

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```
