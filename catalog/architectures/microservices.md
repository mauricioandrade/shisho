---
id: microservices
nome: Microsserviços
familia: distribuida
assinatura:
  pacotes: []
  anotacoes: ["multiplos_modulos_de_build_com_main", "docker-compose_com_varios_servicos_spring", "spring-cloud", "@FeignClient", "RestClient_para_servico_interno"]
  minimo_sinais: 2
regras_dependencia:
  - de: "<servico>"
    nao_pode_depender_de: ["banco:<outro-servico>"]   # cada serviço é dono dos seus dados
  - de: "<servico>"
    nao_pode_depender_de: ["codigo:<outro-servico>"]  # sem módulo de domínio compartilhado
custo:
  classes_por_feature: 8-15
  complexidade: alta
faz_sentido_quando:
  - multiplos_times
  - escala_independente_por_dominio
  - time_grande
sinais_overengineering:
  - servicos_compartilhando_banco
  - modulo_common_com_entidades_compartilhadas
  - chamadas_sincronas_em_cadeia
  - mais_servicos_que_desenvolvedores
  - servicos_sempre_deployados_juntos
sinais_subengenharia: []
evolui_para: []
alternativa_mais_simples: modular-monolith
referencias:
  - https://martinfowler.com/articles/microservices.html
  - https://martinfowler.com/bliki/MonolithFirst.html
---

## O que é

O sistema é dividido em **serviços implantados de forma independente**, cada um dono do seu
domínio e dos seus dados, comunicando-se pela rede.

## Quando vale o custo

É, antes de tudo, uma solução **organizacional**: vários times que precisam entregar sem se
bloquear, partes do sistema com necessidades de escala muito diferentes. Sem esses problemas,
os custos (rede, consistência eventual, observabilidade distribuída, deploy) não se pagam.

## Quando é exagero

Os sinais mais comuns: serviços que leem o mesmo banco, um módulo `common` com entidades
compartilhadas, serviços que só funcionam se forem deployados juntos, ou mais serviços do que
pessoas no time. Isso é um **monolito distribuído**: os custos dos microsserviços sem os
benefícios. O caminho de volta é o monolito modular.
