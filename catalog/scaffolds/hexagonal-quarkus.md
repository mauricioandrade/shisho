---
id: hexagonal-quarkus
arquitetura: hexagonal
grupo_do_build: io.quarkus
dependencias: ["quarkus-rest-jackson|quarkus-rest-jsonb", quarkus-hibernate-validator, quarkus-hibernate-orm-panache]
uma_vez_por_projeto:
  adapter/in/web/TratadorDeErros.java: ServerExceptionMapper
---

## domain/{{Entidade}}.java

O coração do hexágono: um {{Entidade}} em Java puro, sem JPA e sem Quarkus. É o que permite testar
as regras de negócio com `new`, sem banco e sem subir a aplicação, e trocar a persistência sem
tocar aqui. O preço aparece logo: a tabela precisa de outra classe, a `{{Entidade}}Entity`.
Toda mudança passa por `atualizar()`, e é ali que uma regra entra quando aparecer. Se ela nunca
aparecer, o núcleo protege um domínio que não existe, e o package-by-feature faria o mesmo
com menos arquivos. O `id` é nulo até o {{Entidade}} ser salvo, porque quem o gera é o banco.

```java
package {{pacote}}.domain;

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

public class {{Entidade}} {

    private final Long id;
{{#campos}}
    private {{tipo}} {{campo}};
{{/campos}}

    public {{Entidade}}(Long id, {{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}}) {
        this.id = id;
{{#campos}}
        this.{{campo}} = {{campo}};
{{/campos}}
    }

    /** Um {{Entidade}} que ainda não foi salvo. */
    public static {{Entidade}} novo({{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}}) {
        return new {{Entidade}}(null, {{#campos}}{{campo}}{{,}}{{/campos}});
    }

    public void atualizar({{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}}) {
{{#campos}}
        this.{{campo}} = {{campo}};
{{/campos}}
    }

    public Long getId() {
        return id;
    }
{{#campos}}

    public {{tipo}} get{{Campo}}() {
        return {{campo}};
    }
{{/campos}}
}
```

## domain/{{Entidade}}InexistenteException.java

O erro de buscar um {{Entidade}} que não existe. É uma `RuntimeException` comum: o domínio não
sabe o que é HTTP, então não fala em 404. Quem traduz o erro para a web é o adaptador web, no
`{{Entidade}}Resource`.

```java
package {{pacote}}.domain;

public class {{Entidade}}InexistenteException extends RuntimeException {

    public {{Entidade}}InexistenteException(Long id) {
        super("{{Entidade}} " + id + " não existe");
    }
}
```

## port/in/{{Entidade}}Command.java

A entrada do núcleo: os dados para criar ou atualizar um {{Entidade}}, sem `id`. Hoje ele também
é o corpo da requisição HTTP, porque os dois formatos são iguais. No dia em que a API precisar de
um formato diferente do caso de uso (outro nome de campo, uma versão nova), crie um
`{{Entidade}}Request` no adaptador web e converta. Antes disso, seria uma cópia sem motivo.

```java
package {{pacote}}.port.in;

{{#validacoes}}
import jakarta.validation.constraints.{{validacao}};
{{/validacoes}}

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

public record {{Entidade}}Command({{#campos}}
        @{{validacao}} {{tipo}} {{campo}}{{,}}{{/campos}}) {
}
```

## port/in/{{Entidade}}UseCase.java

A porta de entrada: o que o mundo de fora pode pedir ao núcleo. O resource depende desta
interface, e não do service. Uma porta para as operações de {{Entidade}}, e não um use case por
operação (`Criar{{Entidade}}UseCase`, `Buscar{{Entidade}}UseCase`…): cinco interfaces de um método só
num CRUD é um dos sinais de overengineering que o Shishō mede.

```java
package {{pacote}}.port.in;

import {{pacote}}.domain.{{Entidade}};

import java.util.List;

public interface {{Entidade}}UseCase {

    List<{{Entidade}}> listar();

    {{Entidade}} buscar(Long id);

    {{Entidade}} criar({{Entidade}}Command command);

    {{Entidade}} atualizar(Long id, {{Entidade}}Command command);

    void remover(Long id);
}
```

## port/out/{{Entidade}}Repository.java

A porta de saída: o que o núcleo precisa do mundo de fora, nos termos dele. Não há `PanacheRepository`
aqui, só o que o service usa. Num teste, uma implementação em memória com um `HashMap` substitui
o banco. Se nenhum teste fizer isso e só existir a implementação JPA, a porta é uma interface a
mais sem uso: é o sinal clássico de hexagonal sem necessidade.

```java
package {{pacote}}.port.out;

import {{pacote}}.domain.{{Entidade}};

import java.util.List;
import java.util.Optional;

public interface {{Entidade}}Repository {

    List<{{Entidade}}> todos();

    Optional<{{Entidade}}> porId(Long id);

    {{Entidade}} salvar({{Entidade}} {{entidade}});

    void remover(Long id);
}
```

## application/{{Entidade}}Service.java

O caso de uso: implementa a porta de entrada usando a de saída, sem saber que existe HTTP ou JPA.
Repare que `atualizar` chama `salvar`. Nas outras arquiteturas não precisa, porque o Hibernate
grava sozinho o que mudou na entidade. Aqui o domínio não é uma entidade JPA, e o Hibernate não o
acompanha. A classe é package-private: ninguém a usa diretamente, só pela porta. O
`@ApplicationScoped`, o `@Inject` e o `@Transactional` são anotações do Jakarta EE (CDI e JTA), e
não do Quarkus, mas continuam sendo framework no núcleo, uma concessão: tirá-las pediria um
produtor CDI (`@Produces`) para cada service.

```java
package {{pacote}}.application;

import {{pacote}}.domain.{{Entidade}};
import {{pacote}}.domain.{{Entidade}}InexistenteException;
import {{pacote}}.port.in.{{Entidade}}Command;
import {{pacote}}.port.in.{{Entidade}}UseCase;
import {{pacote}}.port.out.{{Entidade}}Repository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
class {{Entidade}}Service implements {{Entidade}}UseCase {

    @Inject
    {{Entidade}}Repository repository;

    @Override
    public List<{{Entidade}}> listar() {
        return repository.todos();
    }

    @Override
    public {{Entidade}} buscar(Long id) {
        return repository.porId(id).orElseThrow(() -> new {{Entidade}}InexistenteException(id));
    }

    @Override
    @Transactional
    public {{Entidade}} criar({{Entidade}}Command command) {
        return repository.salvar({{Entidade}}.novo({{#campos}}command.{{campo}}(){{,}}{{/campos}}));
    }

    @Override
    @Transactional
    public {{Entidade}} atualizar(Long id, {{Entidade}}Command command) {
        {{Entidade}} {{entidade}} = buscar(id);
        {{entidade}}.atualizar({{#campos}}command.{{campo}}(){{,}}{{/campos}});
        return repository.salvar({{entidade}});
    }

    @Override
    @Transactional
    public void remover(Long id) {
        buscar(id);
        repository.remover(id);
    }
}
```

## adapter/out/persistence/{{Entidade}}Entity.java

A tabela, vista pelo JPA. Tem os mesmos campos do {{Entidade}} do domínio: é a duplicação que o
hexagonal cobra para manter o domínio livre do banco. A conversão mora aqui mesmo (`de` e
`paraDominio`), sem uma classe `Mapper` a mais. Como tudo neste pacote, a classe é
package-private: fora do adaptador, ninguém sabe que existe JPA.

```java
package {{pacote}}.adapter.out.persistence;

import {{pacote}}.domain.{{Entidade}};
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

@Entity
@Table(name = "{{tabela}}")
class {{Entidade}}Entity {

    @Id
    @GeneratedValue
    private Long id;
{{#campos}}
    private {{tipo}} {{campo}};
{{/campos}}

    protected {{Entidade}}Entity() {
    }

    {{Entidade}}Entity({{Entidade}} {{entidade}}) {
        this.id = {{entidade}}.getId();
{{#campos}}
        this.{{campo}} = {{entidade}}.get{{Campo}}();
{{/campos}}
    }

    {{Entidade}} paraDominio() {
        return new {{Entidade}}(id, {{#campos}}{{campo}}{{,}}{{/campos}});
    }
}
```

## adapter/out/persistence/{{Entidade}}PanacheRepository.java

O Panache, escondido dentro do adaptador. Ele trabalha com a `{{Entidade}}Entity`, nunca com o
domínio.

```java
package {{pacote}}.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class {{Entidade}}PanacheRepository implements PanacheRepository<{{Entidade}}Entity> {
}
```

## adapter/out/persistence/{{Entidade}}PersistenceAdapter.java

O adaptador de saída: implementa a porta `{{Entidade}}Repository` com JPA, convertendo entre o
domínio e a entidade em cada chamada. Um {{Entidade}} sem `id` é novo e vai para `persist`; um com
`id` já existe, e o `merge` copia o que mudou para a linha da tabela. Trocar o banco por outra
coisa (um serviço HTTP, um arquivo) é escrever outro adaptador como este, sem mexer no núcleo. É
esse o ganho que paga os arquivos a mais.

```java
package {{pacote}}.adapter.out.persistence;

import {{pacote}}.domain.{{Entidade}};
import {{pacote}}.port.out.{{Entidade}}Repository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
class {{Entidade}}PersistenceAdapter implements {{Entidade}}Repository {

    @Inject
    {{Entidade}}PanacheRepository panache;

    @Override
    public List<{{Entidade}}> todos() {
        return panache.listAll().stream().map({{Entidade}}Entity::paraDominio).toList();
    }

    @Override
    public Optional<{{Entidade}}> porId(Long id) {
        return panache.findByIdOptional(id).map({{Entidade}}Entity::paraDominio);
    }

    @Override
    public {{Entidade}} salvar({{Entidade}} {{entidade}}) {
        {{Entidade}}Entity entity = new {{Entidade}}Entity({{entidade}});
        if ({{entidade}}.getId() == null) {
            panache.persist(entity);
            return entity.paraDominio();
        }
        return panache.getEntityManager().merge(entity).paraDominio();
    }

    @Override
    public void remover(Long id) {
        panache.deleteById(id);
    }
}
```

## adapter/in/web/{{Entidade}}Response.java

O que a API devolve. Separar a resposta do domínio faz do contrato da API uma decisão sua:
mudar o {{Entidade}} deixa de quebrar os clientes.

```java
package {{pacote}}.adapter.in.web;

import {{pacote}}.domain.{{Entidade}};

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

record {{Entidade}}Response(Long id{{#campos}}, {{tipo}} {{campo}}{{/campos}}) {

    static {{Entidade}}Response de({{Entidade}} {{entidade}}) {
        return new {{Entidade}}Response({{entidade}}.getId(){{#campos}}, {{entidade}}.get{{Campo}}(){{/campos}});
    }
}
```

## adapter/in/web/{{Entidade}}Resource.java

O adaptador de entrada: traduz HTTP para a porta `{{Entidade}}UseCase` e de volta. Depende da
porta, e não do service. É aqui que o erro do domínio vira 404, num `@ServerExceptionMapper` que
só vale para este resource, porque status HTTP é assunto da web. Os status de sucesso também
ficam aqui: 201 ao criar; um método `void` já responde 204. A classe é `public` porque a Quarkus
REST só publica endpoints em métodos públicos.

```java
package {{pacote}}.adapter.in.web;

import {{pacote}}.domain.{{Entidade}}InexistenteException;
import {{pacote}}.port.in.{{Entidade}}Command;
import {{pacote}}.port.in.{{Entidade}}UseCase;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response.Status;
import org.jboss.resteasy.reactive.ResponseStatus;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

import java.util.List;

@Path("/{{rota}}")
public class {{Entidade}}Resource {

    @Inject
    {{Entidade}}UseCase useCase;

    @GET
    public List<{{Entidade}}Response> listar() {
        return useCase.listar().stream().map({{Entidade}}Response::de).toList();
    }

    @GET
    @Path("/{id}")
    public {{Entidade}}Response buscar(Long id) {
        return {{Entidade}}Response.de(useCase.buscar(id));
    }

    @POST
    @ResponseStatus(201)
    public {{Entidade}}Response criar(@Valid {{Entidade}}Command command) {
        return {{Entidade}}Response.de(useCase.criar(command));
    }

    @PUT
    @Path("/{id}")
    public {{Entidade}}Response atualizar(Long id, @Valid {{Entidade}}Command command) {
        return {{Entidade}}Response.de(useCase.atualizar(id, command));
    }

    @DELETE
    @Path("/{id}")
    public void remover(Long id) {
        useCase.remover(id);
    }

    @ServerExceptionMapper
    RestResponse<Problema> inexistente({{Entidade}}InexistenteException e) {
        Problema problema = new Problema("Não encontrado", Status.NOT_FOUND.getStatusCode(), e.getMessage());
        return RestResponse.ResponseBuilder.create(Status.NOT_FOUND, problema).type("application/problem+json").build();
    }

    /** O formato de erro da RFC 9457. */
    record Problema(String title, int status, String detail) {
    }
}
```

## adapter/in/web/TratadorDeErros.java

Um lugar só para os erros da API, criado uma vez por projeto, no adaptador web: erro em formato
HTTP é assunto dele. Cada método `@ServerExceptionMapper` traduz um tipo de exceção numa
resposta; este responde no formato da RFC 9457 (`application/problem+json`) aos 404 que não vêm
do domínio, como uma rota que não existe. Os erros de validação já saem como 400 com a lista dos
campos que falharam, pela extensão `quarkus-hibernate-validator`.

```java
package {{pacote}}.adapter.in.web;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response.Status;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

class TratadorDeErros {

    @ServerExceptionMapper
    RestResponse<Problema> naoEncontrado(NotFoundException e) {
        Problema problema = new Problema("Não encontrado", Status.NOT_FOUND.getStatusCode(), e.getMessage());
        return RestResponse.ResponseBuilder.create(Status.NOT_FOUND, problema).type("application/problem+json").build();
    }

    /** O formato de erro da RFC 9457. */
    record Problema(String title, int status, String detail) {
    }
}
```
