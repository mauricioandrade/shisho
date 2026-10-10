---
id: cqrs
nome: CQRS (Command Query Responsibility Segregation)
familia: isolamento-de-dominio
assinatura:
  pacotes: ["command|comando", "query|consulta", "commands|comandos", "queries|consultas", "read|leitura/model|modelo", "write|escrita/model|modelo"]
  anotacoes: ["CommandHandler", "QueryHandler", "org.axonframework"]
  minimo_sinais: 2
regras_dependencia:
  - de: "query|consulta"
    nao_pode_depender_de: ["command|comando"]
  - de: "command|comando"
    nao_pode_depender_de: ["query|consulta/model|modelo"]
custo:
  classes_por_feature: 8-14
  complexidade: alta
faz_sentido_quando:
  - carga_leitura_escrita_assimetrica
  - dominio_com_regras_complexas
  - auditoria_de_eventos
sinais_overengineering:
  - modelo_de_leitura_identico_ao_de_escrita
  - mesmo_banco_mesmas_tabelas_para_leitura_e_escrita
  - command_bus_com_um_handler_por_comando_sem_middleware
  - aplicado_ao_sistema_inteiro
sinais_subengenharia: []
evolui_para: []
alternativa_mais_simples: package-by-feature
referencias:
  - https://martinfowler.com/bliki/CQRS.html
---

## O que é

Separar o modelo que **altera** estado (comandos) do modelo que **lê** estado (consultas). Em
casos extremos, cada lado tem sua própria base, e o de leitura é atualizado por eventos.

## Quando vale o custo

Quando leitura e escrita têm formas e cargas muito diferentes: telas que agregam dados de
vários lugares, relatórios pesados, volume de leitura ordens de grandeza maior. E quase
sempre **em uma parte** do sistema, não nele todo.

## Quando é exagero

Se o modelo de leitura é igual ao de escrita e os dois usam as mesmas tabelas, o projeto só
ganhou o dobro de classes. Uma versão leve já resolve a maioria dos casos: um método de
consulta com projeção (record) e `@Transactional(readOnly = true)`, sem barramento nenhum.
