---
id: clean
nome: Clean Architecture
familia: isolamento-de-dominio
assinatura:
  pacotes: [domain, application, infrastructure, usecase, "domain/model", "domain/port"]
  minimo_sinais: 3
regras_dependencia:
  - de: domain
    nao_pode_depender_de: [application, infrastructure, "org.springframework", "jakarta.persistence"]
  - de: application
    nao_pode_depender_de: [infrastructure]
custo:
  classes_por_feature: 7-12
  complexidade: alta
faz_sentido_quando:
  - dominio_com_regras_complexas
  - vida_util_longa
  - troca_provavel_de_infra
  - time_grande
sinais_overengineering:
  - use_cases_que_so_delegam
  - mappers_entre_estruturas_identicas
  - dominio_anemico
  - entidade_de_dominio_identica_a_entidade_jpa
  - um_use_case_por_metodo_crud
sinais_subengenharia: []
evolui_para: [modular-monolith]
alternativa_mais_simples: package-by-feature
referencias:
  - https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html
---

## O que é

Círculos concêntricos com uma única regra: **dependências apontam para dentro**. Entidades no
centro, casos de uso em volta, adaptadores de interface e frameworks na borda. O domínio não
sabe que existe Spring, JPA ou HTTP.

## Diferença prática para Hexagonal

Os objetivos são os mesmos (núcleo isolado, infraestrutura plugável). Clean é mais prescritiva
nas camadas e costuma gerar mais classes; Hexagonal fala só de portas e adaptadores. O Shishō
classifica pelos pacotes, mas trata as duas na mesma família.

## Quando é exagero

É a arquitetura mais citada em projetos de portfólio e a que mais aparece como
overengineering. Para cada endpoint CRUD: request, comando, use case (interface + impl),
entidade de domínio, entidade JPA, dois mappers, response. Se o domínio não tem invariantes a
proteger, todo esse aparato só copia campos de um lado para o outro.
