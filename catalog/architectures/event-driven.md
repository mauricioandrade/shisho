---
id: event-driven
nome: Event-Driven
familia: distribuida
assinatura:
  pacotes: ["event|evento", "events|eventos", listener, "consumer|consumidor", "producer|produtor", "messaging|mensageria"]
  anotacoes: ["@KafkaListener", "@RabbitListener", "@EventListener", "@TransactionalEventListener", "spring-kafka", "spring-amqp", "spring-cloud-stream"]
  minimo_sinais: 2
regras_dependencia:
  - de: "<produtor>"
    nao_pode_depender_de: ["<consumidor>"]   # quem publica não conhece quem consome
custo:
  classes_por_feature: 6-10
  complexidade: alta
faz_sentido_quando:
  - processamento_assincrono
  - multiplas_integracoes_externas
  - multiplos_times
  - auditoria_de_eventos
sinais_overengineering:
  - broker_externo_com_produtor_e_consumidor_no_mesmo_servico
  - evento_com_um_unico_consumidor_que_poderia_ser_chamada_direta
  - fluxo_sincrono_disfarcado   # publica e espera a resposta
sinais_subengenharia: []
evolui_para: []
alternativa_mais_simples: modular-monolith
referencias:
  - https://martinfowler.com/articles/201701-event-driven.html
---

## O que é

Componentes reagem a **eventos** ("pedido criado") em vez de chamarem uns aos outros
diretamente. Quem publica não sabe quem consome. Pode ser interno (eventos de aplicação do
Spring) ou distribuído (Kafka, RabbitMQ).

## Quando vale o custo

Trabalho que pode ser assíncrono (e-mail, notificações, integrações lentas), vários
interessados no mesmo fato, sistemas de times diferentes que precisam se desacoplar.

## Quando é exagero

Um Kafka para um serviço que publica e consome os próprios eventos acrescenta infraestrutura,
consistência eventual e depuração difícil, sem desacoplar nada. Dentro de um monolito,
`ApplicationEventPublisher` com `@TransactionalEventListener` entrega o mesmo desacoplamento
de código, sem broker.
