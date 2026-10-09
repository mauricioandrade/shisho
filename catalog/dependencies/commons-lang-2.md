---
id: commons-lang-2
nome: Apache Commons Lang 2
coordenadas:
  - commons-lang:commons-lang
resumo: substituído pelo Commons Lang 3, e sem versão nova desde 2011
alternativa: a biblioteca padrão (Objects, String.isBlank, String.repeat) ou o Commons Lang 3
referencias:
  - https://commons.apache.org/proper/commons-lang/
  - https://central.sonatype.com/artifact/commons-lang/commons-lang
---

## Por quê

A versão 2.6, de 2011, é a última da linha 2. O Commons Lang 3 é o sucessor, num pacote
diferente (`org.apache.commons.lang3`), justamente para os dois poderem conviver durante a
migração.

Antes de trocar uma pela outra, vale olhar o JDK: boa parte do que se usava do Commons Lang
virou API padrão (`Objects.requireNonNull`, `Objects.equals`, `String.isBlank`, `String.strip`,
`String.repeat`). Às vezes a melhor migração é remover a dependência.
