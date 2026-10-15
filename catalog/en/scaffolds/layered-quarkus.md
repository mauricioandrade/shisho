---
id: layered-quarkus
uma_vez_por_projeto:
  resource/ErrorHandler.java: ServerExceptionMapper
---

## model/{{Entidade}}.java

The JPA entity: the database table seen as an object. The no-argument constructor is
`protected` because only Hibernate should use it; whoever creates a {{Entidade}} goes through
the constructor with the fields. The table has its own name (`{{tabela}}`) so it does not clash
with SQL reserved words, such as `user` and `order`.
There are no setters: every change goes through `update()`, and that is where a business rule
goes when one shows up.

```java
package {{pacote}}.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

@Entity
@Table(name = "{{tabela}}")
public class {{Entidade}} {

    @Id
    @GeneratedValue
    private Long id;
{{#campos}}
    private {{tipo}} {{campo}};
{{/campos}}

    protected {{Entidade}}() {
    }

    public {{Entidade}}({{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}}) {
{{#campos}}
        this.{{campo}} = {{campo}};
{{/campos}}
    }

    public void update({{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}}) {
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

## repository/{{Entidade}}Repository.java

The database access. Implementing `PanacheRepository` already gives you `listAll`,
`findByIdOptional`, `persist` and `delete`, without a line of SQL. New queries become methods
here, such as `find("name", name).list()`, with the condition written in simplified HQL.

```java
package {{pacote}}.repository;

import {{pacote}}.model.{{Entidade}};
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class {{Entidade}}Repository implements PanacheRepository<{{Entidade}}> {
}
```

## dto/{{Entidade}}Request.java

What the client sends to create or update. It is a record, immutable and free of boilerplate,
and not the entity: that way the client cannot send an `id` or internal fields. The validation
annotations only count because the resource uses `@Valid`.

```java
package {{pacote}}.dto;

{{#validacoes}}
import jakarta.validation.constraints.{{validacao}};
{{/validacoes}}

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

public record {{Entidade}}Request({{#campos}}
        @{{validacao}} {{tipo}} {{campo}}{{,}}{{/campos}}) {
}
```

## dto/{{Entidade}}Response.java

What the API returns. Keeping the response apart from the entity makes the API contract your
decision, not a reflection of the database schema. Changing a column no longer breaks the
clients.

```java
package {{pacote}}.dto;

import {{pacote}}.model.{{Entidade}};

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

public record {{Entidade}}Response(Long id{{#campos}}, {{tipo}} {{campo}}{{/campos}}) {

    public static {{Entidade}}Response of({{Entidade}} {{entidade}}) {
        return new {{Entidade}}Response({{entidade}}.getId(){{#campos}}, {{entidade}}.get{{Campo}}(){{/campos}});
    }
}
```

## service/{{Entidade}}NotFoundException.java

The error of looking up a {{Entidade}} that does not exist. Extending Jakarta REST's
`NotFoundException` makes Quarkus answer 404 with no extra code. The cost is the service knowing
about HTTP. If another channel (a queue, a job) starts using the service, switch to an exception
with no HTTP and translate it in the ErrorHandler.

```java
package {{pacote}}.service;

import jakarta.ws.rs.NotFoundException;

public class {{Entidade}}NotFoundException extends NotFoundException {

    public {{Entidade}}NotFoundException(Long id) {
        super("{{Entidade}} " + id + " does not exist");
    }
}
```

## service/{{Entidade}}Service.java

Where the business rules live and where the transaction starts: only the methods that write are
`@Transactional`. Notice that `update` does not call `persist`: inside the transaction, Hibernate
writes whatever changed in the entity by itself. The repository arrives through `@Inject` on a
package-private field, the style Quarkus recommends: ArC fills the field with code generated at
build time, with no reflection.

```java
package {{pacote}}.service;

import {{pacote}}.dto.{{Entidade}}Request;
import {{pacote}}.dto.{{Entidade}}Response;
import {{pacote}}.model.{{Entidade}};
import {{pacote}}.repository.{{Entidade}}Repository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class {{Entidade}}Service {

    @Inject
    {{Entidade}}Repository repository;

    public List<{{Entidade}}Response> list() {
        return repository.listAll().stream().map({{Entidade}}Response::of).toList();
    }

    public {{Entidade}}Response find(Long id) {
        return {{Entidade}}Response.of(load(id));
    }

    @Transactional
    public {{Entidade}}Response create({{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = new {{Entidade}}({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        repository.persist({{entidade}});
        return {{Entidade}}Response.of({{entidade}});
    }

    @Transactional
    public {{Entidade}}Response update(Long id, {{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = load(id);
        {{entidade}}.update({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        return {{Entidade}}Response.of({{entidade}});
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(load(id));
    }

    private {{Entidade}} load(Long id) {
        return repository.findByIdOptional(id).orElseThrow(() -> new {{Entidade}}NotFoundException(id));
    }
}
```

## resource/{{Entidade}}Resource.java

The HTTP door, which Jakarta REST calls a resource. It only translates: it reads the request,
calls the service and picks the status (201 on create; a `void` method already answers 204).
Business rules here are a sign they escaped from the service. The `id` parameter comes from the
path by its name, with no `@PathParam`, and the body is the parameter with no annotation.

```java
package {{pacote}}.resource;

import {{pacote}}.dto.{{Entidade}}Request;
import {{pacote}}.dto.{{Entidade}}Response;
import {{pacote}}.service.{{Entidade}}Service;
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
    public List<{{Entidade}}Response> list() {
        return service.list();
    }

    @GET
    @Path("/{id}")
    public {{Entidade}}Response find(Long id) {
        return service.find(id);
    }

    @POST
    @ResponseStatus(201)
    public {{Entidade}}Response create(@Valid {{Entidade}}Request request) {
        return service.create(request);
    }

    @PUT
    @Path("/{id}")
    public {{Entidade}}Response update(Long id, @Valid {{Entidade}}Request request) {
        return service.update(id, request);
    }

    @DELETE
    @Path("/{id}")
    public void delete(Long id) {
        service.delete(id);
    }
}
```

## resource/ErrorHandler.java

A single place for the API's errors, created once per project. Each `@ServerExceptionMapper`
method turns an exception type into a response; this one answers every 404,
{{Entidade}}NotFoundException's included, in the RFC 9457 format (`application/problem+json`).
Validation errors already come out as 400 with the list of fields that failed, thanks to the
`quarkus-hibernate-validator` extension. A new domain error gets a method here, not a try/catch in
every endpoint.

```java
package {{pacote}}.resource;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response.Status;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

class ErrorHandler {

    @ServerExceptionMapper
    RestResponse<Problem> notFound(NotFoundException e) {
        Problem problem = new Problem("Not found", Status.NOT_FOUND.getStatusCode(), e.getMessage());
        return RestResponse.ResponseBuilder.create(Status.NOT_FOUND, problem).type("application/problem+json").build();
    }

    /** The RFC 9457 error format. */
    record Problem(String title, int status, String detail) {
    }
}
```
