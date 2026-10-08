---
id: onion
nome: Onion Architecture
familia: isolamento-de-dominio
assinatura:
  pacotes: [core, "domain/model", "domain/services", "application/services", infrastructure]
  minimo_sinais: 3
regras_dependencia:
  - de: core
    nao_pode_depender_de: [infrastructure, "org.springframework", "jakarta.persistence"]
  - de: "domain/model"
    nao_pode_depender_de: ["domain/services", "application/services"]
custo:
  classes_por_feature: 6-10
  complexidade: media
faz_sentido_quando:
  - dominio_com_regras_complexas
  - vida_util_longa
  - troca_provavel_de_infra
sinais_overengineering:
  - camadas_de_servico_que_so_delegam
  - interfaces_de_repositorio_com_uma_implementacao_sem_fake_em_teste
  - dominio_anemico
sinais_subengenharia: []
evolui_para: [modular-monolith]
alternativa_mais_simples: package-by-feature
referencias:
  - https://jeffreypalermo.com/2008/07/the-onion-architecture-part-1/
---

## O que é

Camadas concêntricas em torno do **modelo de domínio**: modelo no centro, serviços de domínio,
serviços de aplicação e, na casca externa, infraestrutura, UI e testes. As interfaces de
repositório ficam no núcleo; as implementações, na casca.

## Relação com Clean e Hexagonal

É anterior à Clean Architecture e muito parecida com ela. A diferença mais visível é a
separação explícita entre serviços de domínio e serviços de aplicação.

## Quando é exagero

Os mesmos sinais da família: camadas de serviço que só repassam chamadas e um "modelo de
domínio" que é só getter e setter.
