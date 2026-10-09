# ADR 0006 — CRUD hexagonal e o aviso de custo antes de gerar

- **Status:** aceito
- **Data:** 2026-10-09
- **Fase:** 7 (gerador de CRUD)

## Contexto

O `shisho crud` (ADR 0005) começou com `layered` e `package-by-feature`, as duas arquiteturas de
custo baixo do catálogo. O próximo template é o `hexagonal`, de custo médio. Ele traz um problema
que os primeiros não tinham: a ferramenta que aponta overengineering passaria a gerar, com um
comando, a arquitetura que o próprio catálogo chama de exagero para um CRUD sem regras.

Não oferecer o template seria esconder a arquitetura de quem quer aprendê-la. Oferecer sem dizer
nada seria ensinar o hábito errado.

## Decisão

### 1. O aviso vem antes, e o padrão é não gravar

Quando o CRUD pede uma arquitetura de complexidade média ou alta, o Shishō olha o `contexto` do
`shisho.yaml`. Se nenhuma chave declarada está em `faz_sentido_quando` da arquitetura, ou se não
há contexto nenhum, ele **não grava nada**. Mostra quantos arquivos o CRUD criaria, quando a
arquitetura vale a pena e qual é a alternativa mais simples. Sai com código 2.

Há duas saídas:

- **Declarar o motivo** em `contexto.caracteristicas`, como `dominio_com_regras_complexas`. É
  o caminho que o Shishō quer ensinar: a decisão fica escrita no repositório, e o `analyze`
  passa a usá-la no veredito.
- **`--mesmo-assim`**, para quem só quer ver como a arquitetura fica. O CRUD é gerado, e o aviso
  aparece no relatório.

Com o contexto justificando a escolha, o CRUD sai direto, e o relatório diz qual chave pagou o
custo.

O critério é o mesmo do veredito do `analyze` (ADR 0002): custo acima de baixo, e nada no
contexto que o pague. Gerar e julgar usam a mesma régua.

### 2. Só quando a arquitetura está sendo escolhida agora

Se o projeto já é hexagonal, o próximo `crud` segue a arquitetura sem perguntar de novo. A
decisão foi tomada no primeiro CRUD, ou antes do Shishō, e repetir o aviso a cada entidade só
ensinaria a ignorá-lo.

### 3. O template hexagonal

```
domain/            Produto (sem JPA nem Spring), ProdutoInexistenteException
port/in/           ProdutoUseCase (a porta de entrada), ProdutoCommand (a entrada do núcleo)
port/out/          ProdutoRepository (a porta de saída)
application/       ProdutoService (implementa a porta de entrada, usa a de saída)
adapter/in/web/    ProdutoController, ProdutoResponse, TratadorDeErros
adapter/out/persistence/  ProdutoEntity (JPA), ProdutoJpaRepository, ProdutoPersistenceAdapter
```

São 11 arquivos por entidade, contra 7 no `package-by-feature`. A diferença é o custo da
arquitetura, e o relatório mostra isso arquivo por arquivo:

- **O domínio não conhece JPA**, e por isso existe a `ProdutoEntity`, com os mesmos campos. A
  conversão fica na própria entidade JPA (`de` e `paraDominio`), sem uma classe `Mapper` a mais.
- **O Hibernate não acompanha o domínio**: o `atualizar` do service precisa chamar `salvar`, ao
  contrário das outras arquiteturas.
- **O domínio não conhece HTTP**: a exceção é uma `RuntimeException` comum, e quem a traduz para
  404 é um `@ExceptionHandler` no controller, dentro do adaptador web.
- **Uma porta de entrada por entidade**, e não um use case por operação, que o catálogo já aponta
  como sinal de overengineering.
- **O comando é o corpo da requisição.** Um `ProdutoRequest` no adaptador web separaria o formato
  HTTP do formato do caso de uso, mas hoje os dois são idênticos. O texto do arquivo diz quando
  separar.
- **O service usa `@Service` e `@Transactional`.** As regras do catálogo proíbem Spring no
  `domain`, não na `application`. Um núcleo sem nenhum Spring pediria uma classe de configuração
  com `@Bean` para cada service: mais uma classe, sem ganho num CRUD.

Os sinais de overengineering que o Shishō mede no hexagonal (`dominio_anemico`,
`mappers_entre_estruturas_identicas`) não aparecem no código gerado porque ele não tem esses
defeitos: o domínio tem comportamento (`atualizar`) e não há mapper entre estruturas iguais.
Isso não esconde o custo; o custo é o aviso.

## O teste-oráculo

Continua o mesmo do ADR 0005, agora também para o hexagonal: o CRUD gerado é reconhecido como
hexagonal, não viola nenhuma regra de dependência e não tem nenhuma lição.

## Alternativas registradas

- **Só avisar, e gravar mesmo assim.** Um aviso depois de criados onze arquivos chega tarde.
- **Pergunta interativa (s/N).** Quebra o uso em scripts e em testes, e o Shishō não tem nenhum
  comando interativo hoje.
- **`clean` junto.** O `clean` do catálogo tem custo alto e uma estrutura (`usecase`,
  `domain/port`) que se sobrepõe à do hexagonal. Fica para quando houver pedido: o mecanismo do
  aviso já serve para ele.

## Consequências

- `shisho crud --arquitetura hexagonal` exige um motivo no `shisho.yaml` ou `--mesmo-assim`.
- Todo template novo de custo médio ou alto ganha o aviso sem código novo: basta o template.

## 📚 Estudo — custo de arquitetura

Toda arquitetura é uma troca: você paga em classes, indireções e conversões agora para ganhar
alguma coisa depois. No hexagonal, o ganho é trocar o banco ou o framework web sem tocar no
domínio, e testar regras de negócio sem subir Spring. Se o domínio é um CRUD, não há regra para
proteger, e o custo fica sem retorno.

Por isso o Shishō pergunta "por quê?" antes de gerar, e não "tem certeza?". "Tem certeza?" só
pede um clique. "Por quê?" pede um motivo, e um motivo escrito no `shisho.yaml` pode ser
discutido no code review e reavaliado quando o projeto mudar.
