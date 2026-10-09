---
id: log4j-1
nome: Log4j 1.x
coordenadas:
  - log4j:log4j
resumo: abandonado desde 2015, com vulnerabilidades conhecidas que nunca serão corrigidas
alternativa: SLF4J com Logback, o padrão do Spring Boot, ou Log4j 2
referencias:
  - https://logging.apache.org/log4j/1.x/
  - https://nvd.nist.gov/vuln/detail/CVE-2021-4104
---

## Por quê

O próprio projeto declarou o fim da vida do Log4j 1 em agosto de 2015 e avisa que nenhuma das
falhas listadas será corrigida. A lista inclui execução remota de código via JNDI no
`JMSAppender` (CVE-2021-4104), desserialização insegura no `SocketServer` (CVE-2019-17571) e
injeção de SQL no `JDBCAppender` (CVE-2022-23305).

O Spring Boot já traz SLF4J e Logback configurados: na maioria dos projetos, migrar é trocar a
dependência e os imports. Quem precisa da API do Log4j pode usar o Log4j 2, que é outro projeto,
com outras coordenadas (`org.apache.logging.log4j`).
