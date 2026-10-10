---
id: mysql-connector-java
nome: MySQL Connector/J under its old coordinates
resumo: the coordinates changed in 8.0.31, and the old ones may stop being published without notice
alternativa: com.mysql:mysql-connector-j, the version Spring Boot manages
---

## Why

The driver is the same; what changed is its address. Since version 8.0.31, Connector/J is
published as `com.mysql:mysql-connector-j`, and the old coordinates point to a relocation POM.
The release notes ask for the switch "as soon as possible", because the old ones may be
discontinued at any time.

Spring Boot manages the version of the new coordinates: with them, the driver follows Boot
without declaring a version.
