---
id: package-by-feature-quarkus
uma_vez_por_projeto:
  ErrorHandler.java: ServerExceptionMapper
---

## {{feature}}/{{Entidade}}.java

The JPA entity: the database table seen as an object. Like every class in `{{feature}}`, it is
package-private (no `public`): no other feature sees the entity, and features talk to each other
through the service. It is the cheapest boundary there is, with no framework at all. There are
no setters: every change goes through `update()`. The table has its own name (`{{tabela}}`) so
it does not clash with SQL reserved words, such as `user` and `order`.

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

    void update({{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}}) {
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

The database access, package-private too. Implementing `PanacheRepository` already gives you
`listAll`, `findByIdOptional`, `persist` and `delete`, and simple queries fit in one line, such
as `find("name", name).list()`. Panache also has the Active Record style, where the entity itself
extends `PanacheEntity` and gets those methods. The repository keeps the two apart: the entity
holds the data, and database access has a class of its own.

```java
package {{pacote}}.{{feature}};

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class {{Entidade}}Repository implements PanacheRepository<{{Entidade}}> {
}
```

## {{feature}}/{{Entidade}}Request.java

What the client sends to create or update. It is a record, not the entity: the client cannot
send an `id` or internal fields. The validation annotations only count because the resource
uses `@Valid`.

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

What the API returns. Keeping the response apart from the entity makes the API contract your
decision, not a reflection of the database schema.

```java
package {{pacote}}.{{feature}};

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

record {{Entidade}}Response(Long id{{#campos}}, {{tipo}} {{campo}}{{/campos}}) {

    static {{Entidade}}Response of({{Entidade}} {{entidade}}) {
        return new {{Entidade}}Response({{entidade}}.getId(){{#campos}}, {{entidade}}.get{{Campo}}(){{/campos}});
    }
}
```

## {{feature}}/{{Entidade}}NotFoundException.java

The error of looking up a {{Entidade}} that does not exist. Extending Jakarta REST's
`NotFoundException` makes Quarkus answer 404 with no extra code, and `ErrorHandler` puts the
message in the RFC 9457 format.

```java
package {{pacote}}.{{feature}};

import jakarta.ws.rs.NotFoundException;

class {{Entidade}}NotFoundException extends NotFoundException {

    {{Entidade}}NotFoundException(Long id) {
        super("{{Entidade}} " + id + " does not exist");
    }
}
```

## {{feature}}/{{Entidade}}Service.java

Where the business rules live and where the transaction starts: only the methods that write are
`@Transactional`. Notice that `update` does not call `persist`: inside the transaction, Hibernate
writes whatever changed by itself. The repository arrives through `@Inject` on a package-private
field, the style Quarkus recommends: ArC fills the field with code generated at build time, with
no reflection. When another feature needs a {{Entidade}}, this is the service it uses.

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

    List<{{Entidade}}Response> list() {
        return repository.listAll().stream().map({{Entidade}}Response::of).toList();
    }

    {{Entidade}}Response find(Long id) {
        return {{Entidade}}Response.of(load(id));
    }

    @Transactional
    {{Entidade}}Response create({{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = new {{Entidade}}({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        repository.persist({{entidade}});
        return {{Entidade}}Response.of({{entidade}});
    }

    @Transactional
    {{Entidade}}Response update(Long id, {{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = load(id);
        {{entidade}}.update({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        return {{Entidade}}Response.of({{entidade}});
    }

    @Transactional
    void delete(Long id) {
        repository.delete(load(id));
    }

    private {{Entidade}} load(Long id) {
        return repository.findByIdOptional(id).orElseThrow(() -> new {{Entidade}}NotFoundException(id));
    }
}
```

## {{feature}}/{{Entidade}}Resource.java

The HTTP door, which Jakarta REST calls a resource. It only translates: it reads the request,
calls the service and picks the status (201 on create; a `void` method already answers 204). It
is the feature's only `public` class, because Quarkus REST only publishes endpoints on public
methods. The `id` parameter comes from the path by its name, with no `@PathParam`, and the body
is the parameter with no annotation.

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

## ErrorHandler.java

A single place for the API's errors, created once per project, in the base package: it applies
to every feature. Each `@ServerExceptionMapper` method turns an exception type into a response;
this one answers every 404, {{Entidade}}NotFoundException's included, in the RFC 9457 format
(`application/problem+json`). Validation errors already come out as 400 with the list of fields
that failed, thanks to the `quarkus-hibernate-validator` extension. A new domain error gets a
method here.

```java
package {{pacote}};

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
