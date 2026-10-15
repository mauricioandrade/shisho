# ADR 0014 — `shisho crud` em projetos Quarkus

- **Status:** aceito
- **Data:** 2026-10-15
- **Fase:** 14 (CRUD Quarkus)

## Contexto

Desde a Fase 12 o Shishō analisa projetos Quarkus, mas o `crud` só sabia gerar Spring. Num
projeto Quarkus, ele escrevia `@RestController` e `JpaRepository`, que nem compilam ali. O ADR
0012 deixou isso como ponta aberta.

## Decisão

### 1. Um template por stack, escolhido pelo build

Os templates Quarkus são arquivos próprios no catálogo (`layered-quarkus.md`,
`package-by-feature-quarkus.md`, `hexagonal-quarkus.md`), com dois campos novos:

```yaml
arquitetura: layered         # a arquitetura que ele gera
grupo_do_build: io.quarkus   # usado quando o build declara uma dependência deste groupId
```

O `GenerateCrudService` olha os groupIds do build. Se algum template declara um deles, só os
templates desse stack valem; senão, valem os sem `grupo_do_build`, os de Spring. O `domain` não
ganhou nenhum nome de framework: `Scaffold.buildGroup` é só um groupId, e quem diz que
`io.quarkus` é o Quarkus é o catálogo.

Num projeto Quarkus, uma arquitetura sem template Quarkus é recusada com a lista do que existe
para o stack. Gerar o template Spring ali seria pior do que não gerar nada.

### 2. O código segue o estilo do Quarkus, não uma tradução do Spring

| Papel | Spring | Quarkus |
|---|---|---|
| HTTP | `@RestController` | resource Jakarta REST (`@Path`), `public` |
| Banco | `JpaRepository` | `PanacheRepository` |
| Injeção | construtor, campo `final` | `@Inject` em campo package-private |
| Transação | `@Transactional(readOnly = true)` na classe | `@Transactional` (JTA) só em quem escreve |
| 404 | `ErrorResponseException` / `ProblemDetail` | exceção que estende `NotFoundException` + `@ServerExceptionMapper` |
| Validação | `handleMethodArgumentNotValid` com `campos` | a resposta 400 que a `quarkus-hibernate-validator` já dá |

A injeção em campo é o estilo que a documentação do Quarkus recomenda (ADR 0012), e a kata
`field-injection` não acusa `@Inject` num projeto Quarkus. O `TratadorDeErros` é pulado se alguma
classe **ou método** do projeto já tem `@ServerExceptionMapper`, porque no Quarkus REST a anotação
fica no método.

Nenhum arquivo por entidade depende do `TratadorDeErros`, já que ele pode não ter sido gerado: no
layered e no package-by-feature, a exceção estende `NotFoundException`, e o Quarkus responde 404
mesmo sem tratador. No hexagonal, o domínio não pode conhecer o Jakarta REST, então o próprio
resource traduz o erro do domínio num `@ServerExceptionMapper` local, com o seu record `Problema`.

### 3. O resource é `public`

O package-by-feature do Spring deixa tudo package-private. No Quarkus isso não funciona: o e2e
mostrou que a Quarkus REST ignora métodos de resource package-private, e todas as rotas
respondiam 404. O resource e os seus métodos são `public`; o resto da feature continua
package-private. O template explica o porquê.

### 4. Conferido num projeto real

Além do teste-oráculo (cada template, num projeto como o do code.quarkus.io, é reconhecido como a
sua arquitetura e não tem nenhuma lição), os três templates foram gerados num projeto baixado do
code.quarkus.io (Quarkus 3.40.1, H2), em português e em inglês, compilados com `mvn package`,
executados e testados com `curl`: 200, 201, 204, 400 com os campos, 404 em
`application/problem+json`. Foi esse teste que achou o problema do item 3.

## Alternativas registradas

- **Um template só, com condicionais por framework.** Pediria `{{#quarkus}}` no motor de templates,
  que o ADR 0005 manteve sem condicionais, e deixaria cada arquivo ilegível para quem contribui.
- **Active Record do Panache** (`Produto extends PanacheEntity`, com campos públicos). É idiomático
  e mais curto, mas mistura dados e acesso ao banco e não tem um papel equivalente ao repository
  das outras arquiteturas. Os templates mencionam o estilo e o modo shoshin já o reconhece.
- **Mapear a validação para RFC 9457 com `campos`,** como o template Spring. A resposta padrão do
  Quarkus já diz quais campos falharam; reescrevê-la seria código a mais para o mesmo resultado.
- **Achar o pacote base pelo `groupId` do pom** quando o projeto não tem classes (o
  code.quarkus.io sem código de exemplo). Funcionaria às vezes: nada obriga o pacote a seguir o
  `groupId`. O `--pacote` continua sendo a resposta, e a mensagem de erro já sugere.

## 📚 Estudo — o mesmo padrão, idiomas diferentes

As três arquiteturas são as mesmas nos dois stacks: as camadas, as features e as portas não mudam
quando o framework muda. O que muda é o **idioma** em que cada papel se escreve. O Spring prefere
construtor e anotações próprias; o Quarkus prefere os padrões Jakarta (CDI, JTA, Jakarta REST) e
faz em tempo de build o que o Spring faz quando a aplicação sobe.

Traduzir palavra por palavra (um `@RestController` virando um resource com construtor e campo
`final`) daria código que funciona, mas que um dev Quarkus estranharia. É como traduzir uma
expressão idiomática ao pé da letra. Por isso cada stack tem o seu template, e por isso o teste
de verdade é subir a aplicação: só ali aparece o que a documentação deixa implícito, como a
exigência de métodos públicos no resource.
