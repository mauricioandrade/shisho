---
id: log4j-1
nome: Log4j 1.x
resumo: abandoned since 2015, with known vulnerabilities that will never be fixed
alternativa: SLF4J with Logback, Spring Boot's default, or Log4j 2
---

## Why

The project itself declared Log4j 1's end of life in August 2015 and warns that none of the
listed flaws will be fixed. The list includes remote code execution through JNDI in
`JMSAppender` (CVE-2021-4104), unsafe deserialization in `SocketServer` (CVE-2019-17571) and SQL
injection in `JDBCAppender` (CVE-2022-23305).

Spring Boot already ships SLF4J and Logback configured: in most projects, migrating means
swapping the dependency and the imports. Whoever needs the Log4j API can use Log4j 2, which is a
different project, with different coordinates (`org.apache.logging.log4j`).
