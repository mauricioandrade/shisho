---
id: jackson-1
nome: Jackson 1.x (Codehaus)
coordenadas:
  - org.codehaus.jackson:jackson-mapper-asl
  - org.codehaus.jackson:jackson-core-asl
resumo: sem versão nova desde 2013 e ignorado pelo Spring, que usa o Jackson 2 ou 3
alternativa: o Jackson que o Spring Boot já traz (com.fasterxml.jackson no Boot 3, tools.jackson no Boot 4)
referencias:
  - https://github.com/FasterXML/jackson-1
  - https://central.sonatype.com/artifact/org.codehaus.jackson/jackson-mapper-asl
---

## Por quê

O Jackson 1.x, do pacote `org.codehaus.jackson`, teve a última versão (1.9.13) em 2013. O
repositório dele diz que a linha não é mais desenvolvida ativamente. O Jackson 2 mudou para
`com.fasterxml.jackson` (e o Jackson 3, para `tools.jackson`), e é um desses que o Spring MVC
usa para converter JSON.

Com as duas no classpath, as anotações de uma não valem para a outra: um `@JsonIgnore` importado
de `org.codehaus.jackson` é ignorado em silêncio pelo Spring, e o campo sai no JSON.
