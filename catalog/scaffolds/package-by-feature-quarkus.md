---
id: package-by-feature-quarkus
arquitetura: package-by-feature
grupo_do_build: io.quarkus
dependencias: ["quarkus-rest-jackson|quarkus-rest-jsonb", quarkus-hibernate-validator, quarkus-hibernate-orm-panache]
recusa_imports: [javax.persistence, javax.validation, javax.ws.rs, javax.inject, javax.enterprise, javax.transaction]
plataforma_minima: ["io.quarkus.platform:3", "io.quarkus:3"]
uma_vez_por_projeto:
  TratadorDeErros.java: ServerExceptionMapper
---

## {{feature}}/{{Entidade}}.java

A entidade JPA: a tabela do banco vista como objeto, com as mesmas anotações `jakarta.persistence`
de qualquer projeto Jakarta EE. Como quase todas as classes de `{{feature}}`, ela é
package-private (sem `public`): nenhuma outra feature enxerga a entidade, e as features conversam
pelo service. Não há setters: toda mudança passa por `atualizar()`. A tabela tem nome próprio
(`{{tabela}}`) para não esbarrar em palavras reservadas do SQL, como `user` e `order`.

```java
package {{pacote}}.{{feature}};

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

@Entity
@Table(name = "{{tabela}}")
class {{Entidade}} {

    @Id
    @GeneratedValue
    private Long id;
{{#campos}}
    private {{tipo}} {{campo}};
{{/campos}}

    protected {{Entidade}}() {
    }

    {{Entidade}}({{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}}) {
{{#campos}}
        this.{{campo}} = {{campo}};
{{/campos}}
    }

    void atualizar({{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}}) {
{{#campos}}
        this.{{campo}} = {{campo}};
{{/campos}}
    }

    Long getId() {
        return id;
    }
{{#campos}}

    {{tipo}} get{{Campo}}() {
        return {{campo}};
    }
{{/campos}}
}
```

## {{feature}}/{{Entidade}}Repository.java

O acesso ao banco, também package-private. Implementar `PanacheRepository` já dá `listAll`,
`findByIdOptional`, `persist` e `delete`, e consultas simples cabem numa linha, como
`find("nome", nome).list()`. O Panache também tem o estilo Active Record, em que a própria
entidade estende `PanacheEntity` e ganha esses métodos. O repositório separa as duas coisas: a
entidade guarda os dados, e o acesso ao banco tem uma classe só dele.

```java
package {{pacote}}.{{feature}};

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class {{Entidade}}Repository implements PanacheRepository<{{Entidade}}> {
}
```

## {{feature}}/{{Entidade}}Request.java

O que o cliente envia para criar ou atualizar. É um record, e não a entidade: o cliente não
consegue mandar um `id` nem campos internos. As anotações de validação só valem porque o
resource usa `@Valid`.

```java
package {{pacote}}.{{feature}};

{{#validacoes}}
import jakarta.validation.constraints.{{validacao}};
{{/validacoes}}

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

record {{Entidade}}Request({{#campos}}
        @{{validacao}} {{tipo}} {{campo}}{{,}}{{/campos}}) {
}
```

## {{feature}}/{{Entidade}}Response.java

O que a API devolve. Separar a resposta da entidade faz do contrato da API uma decisão sua, e
não um reflexo do esquema do banco.

```java
package {{pacote}}.{{feature}};

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

record {{Entidade}}Response(Long id{{#campos}}, {{tipo}} {{campo}}{{/campos}}) {

    static {{Entidade}}Response de({{Entidade}} {{entidade}}) {
        return new {{Entidade}}Response({{entidade}}.getId(){{#campos}}, {{entidade}}.get{{Campo}}(){{/campos}});
    }
}
```

## {{feature}}/{{Entidade}}InexistenteException.java

O erro de buscar um {{Entidade}} que não existe. Estender `NotFoundException`, do Jakarta REST, faz
o Quarkus responder 404 sem nenhum código a mais, e o `TratadorDeErros` põe a mensagem no
formato da RFC 9457.

```java
package {{pacote}}.{{feature}};

import jakarta.ws.rs.NotFoundException;

class {{Entidade}}InexistenteException extends NotFoundException {

    {{Entidade}}InexistenteException(Long id) {
        super("{{Entidade}} " + id + " não existe");
    }
}
```

## {{feature}}/{{Entidade}}Service.java

Onde mora a regra de negócio e onde começa a transação: só os métodos que escrevem são
`@Transactional`. Repare que `atualizar` não chama `persist`: dentro da transação, o Hibernate
grava sozinho o que mudou. O repositório chega por `@Inject` num campo package-private, o estilo
que o Quarkus recomenda: o ArC preenche o campo com código gerado na compilação, sem reflexão.
Quando outra feature precisar de um {{Entidade}}, é este service que ela usa.

```java
package {{pacote}}.{{feature}};

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
class {{Entidade}}Service {

    @Inject
    {{Entidade}}Repository repository;

    List<{{Entidade}}Response> listar() {
        return repository.listAll().stream().map({{Entidade}}Response::de).toList();
    }

    {{Entidade}}Response buscar(Long id) {
        return {{Entidade}}Response.de(encontrar(id));
    }

    @Transactional
    {{Entidade}}Response criar({{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = new {{Entidade}}({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        repository.persist({{entidade}});
        return {{Entidade}}Response.de({{entidade}});
    }

    @Transactional
    {{Entidade}}Response atualizar(Long id, {{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = encontrar(id);
        {{entidade}}.atualizar({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        return {{Entidade}}Response.de({{entidade}});
    }

    @Transactional
    void remover(Long id) {
        repository.delete(encontrar(id));
    }

    private {{Entidade}} encontrar(Long id) {
        return repository.findByIdOptional(id).orElseThrow(() -> new {{Entidade}}InexistenteException(id));
    }
}
```

## {{feature}}/{{Entidade}}Resource.java

A porta HTTP, que no Jakarta REST se chama resource. Ela só traduz: lê a requisição, chama o
service e escolhe o status (201 ao criar; um método `void` já responde 204). É a única classe
`public` da feature, porque a Quarkus REST só publica endpoints em métodos públicos. O parâmetro
`id` vem da rota pelo nome, sem `@PathParam`, e o corpo é o parâmetro sem anotação.

```java
package {{pacote}}.{{feature}};

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;

@Path("/{{rota}}")
public class {{Entidade}}Resource {

    @Inject
    {{Entidade}}Service service;

    @GET
    public List<{{Entidade}}Response> listar() {
        return service.listar();
    }

    @GET
    @Path("/{id}")
    public {{Entidade}}Response buscar(Long id) {
        return service.buscar(id);
    }

    @POST
    @ResponseStatus(201)
    public {{Entidade}}Response criar(@Valid {{Entidade}}Request request) {
        return service.criar(request);
    }

    @PUT
    @Path("/{id}")
    public {{Entidade}}Response atualizar(Long id, @Valid {{Entidade}}Request request) {
        return service.atualizar(id, request);
    }

    @DELETE
    @Path("/{id}")
    public void remover(Long id) {
        service.remover(id);
    }
}
```

## TratadorDeErros.java

Um lugar só para os erros da API, criado uma vez por projeto, no pacote base: ele vale para
todas as features. Cada método `@ServerExceptionMapper` traduz um tipo de exceção numa resposta;
este responde a todo 404, inclusive o de {{Entidade}}InexistenteException, no formato da RFC 9457
(`application/problem+json`). Os erros de validação já saem como 400 com a lista dos campos que
falharam, pela extensão `quarkus-hibernate-validator`. Um erro novo do domínio ganha um método
aqui.

```java
package {{pacote}};

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
