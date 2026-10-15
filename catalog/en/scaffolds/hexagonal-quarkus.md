---
id: hexagonal-quarkus
uma_vez_por_projeto:
  adapter/in/web/ErrorHandler.java: ServerExceptionMapper
---

## domain/{{Entidade}}.java

The heart of the hexagon: a {{Entidade}} in plain Java, with no JPA and no Quarkus. That is what
lets you test the business rules with `new`, with no database and without starting the
application, and swap the persistence without touching this class. The price shows up right
away: the table needs another class, `{{Entidade}}Entity`. Every change goes through `update()`,
and that is where a rule goes when one shows up. If one never does, the core protects a domain
that does not exist, and package-by-feature would do the same with fewer files. The `id` is null
until the {{Entidade}} is saved, because the database generates it.

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

    /** A {{Entidade}} that has not been saved yet. */
    public static {{Entidade}} newOne({{#campos}}{{tipo}} {{campo}}{{,}}{{/campos}}) {
        return new {{Entidade}}(null, {{#campos}}{{campo}}{{,}}{{/campos}});
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

## domain/{{Entidade}}NotFoundException.java

The error of looking up a {{Entidade}} that does not exist. It is a plain `RuntimeException`:
the domain does not know what HTTP is, so it does not talk about 404. The one who translates the
error for the web is the web adapter, in `{{Entidade}}Controller`.

```java
package {{pacote}}.domain;

public class {{Entidade}}NotFoundException extends RuntimeException {

    public {{Entidade}}NotFoundException(Long id) {
        super("{{Entidade}} " + id + " does not exist");
    }
}
```

## port/in/{{Entidade}}Command.java

The core's input: the data to create or update a {{Entidade}}, with no `id`. Today it is also the
HTTP request body, because both formats are the same. The day the API needs a format different
from the use case's (another field name, a new version), create a `{{Entidade}}Request` in the
web adapter and convert it. Before that, it would be a copy with no reason.

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

The inbound port: what the outside world may ask of the core. The controller depends on this
interface, not on the service. One port for the {{Entidade}} operations, not one use case per
operation (`Create{{Entidade}}UseCase`, `Find{{Entidade}}UseCase`…): five single-method
interfaces in a CRUD is one of the overengineering signs the Shishō measures.

```java
package {{pacote}}.port.in;

import {{pacote}}.domain.{{Entidade}};

import java.util.List;

public interface {{Entidade}}UseCase {

    List<{{Entidade}}> list();

    {{Entidade}} find(Long id);

    {{Entidade}} create({{Entidade}}Command command);

    {{Entidade}} update(Long id, {{Entidade}}Command command);

    void delete(Long id);
}
```

## port/out/{{Entidade}}Repository.java

The outbound port: what the core needs from the outside world, in its own terms. There is no
`JpaRepository` here, only what the service uses. In a test, an in-memory implementation backed
by a `HashMap` replaces the database. If no test ever does that and the JPA implementation is the
only one, the port is an extra interface with no use: the classic sign of unnecessary hexagonal.

```java
package {{pacote}}.port.out;

import {{pacote}}.domain.{{Entidade}};

import java.util.List;
import java.util.Optional;

public interface {{Entidade}}Repository {

    List<{{Entidade}}> findAll();

    Optional<{{Entidade}}> findById(Long id);

    {{Entidade}} save({{Entidade}} {{entidade}});

    void deleteById(Long id);
}
```

## application/{{Entidade}}Service.java

The use case: it implements the inbound port using the outbound one, without knowing that HTTP or
JPA exist. Notice that `update` calls `save`. In the other architectures it does not need to,
because Hibernate writes whatever changed in the entity by itself. Here the domain is not a JPA
entity, and Hibernate does not track it. The class is package-private: nobody uses it directly,
only through the port. `@ApplicationScoped`, `@Inject` and `@Transactional` are Jakarta EE
annotations (CDI and JTA), not Quarkus ones, but they are still a framework inside the core, a
concession: removing them would take a CDI producer (`@Produces`) for every service.

```java
package {{pacote}}.application;

import {{pacote}}.domain.{{Entidade}};
import {{pacote}}.domain.{{Entidade}}NotFoundException;
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
    public List<{{Entidade}}> list() {
        return repository.findAll();
    }

    @Override
    public {{Entidade}} find(Long id) {
        return repository.findById(id).orElseThrow(() -> new {{Entidade}}NotFoundException(id));
    }

    @Override
    @Transactional
    public {{Entidade}} create({{Entidade}}Command command) {
        return repository.save({{Entidade}}.newOne({{#campos}}command.{{campo}}(){{,}}{{/campos}}));
    }

    @Override
    @Transactional
    public {{Entidade}} update(Long id, {{Entidade}}Command command) {
        {{Entidade}} {{entidade}} = find(id);
        {{entidade}}.update({{#campos}}command.{{campo}}(){{,}}{{/campos}});
        return repository.save({{entidade}});
    }

    @Override
    @Transactional
    public void delete(Long id) {
        find(id);
        repository.deleteById(id);
    }
}
```

## adapter/out/persistence/{{Entidade}}Entity.java

The table, as JPA sees it. It has the same fields as the domain's {{Entidade}}: that is the
duplication hexagonal charges to keep the domain free of the database. The conversion lives right
here (`of` and `toDomain`), with no extra `Mapper` class. Like everything in this package, the
class is package-private: outside the adapter, nobody knows JPA exists.

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

    {{Entidade}} toDomain() {
        return new {{Entidade}}(id, {{#campos}}{{campo}}{{,}}{{/campos}});
    }
}
```

## adapter/out/persistence/{{Entidade}}PanacheRepository.java

Panache, hidden inside the adapter. It works with `{{Entidade}}Entity`, never with the domain.

```java
package {{pacote}}.adapter.out.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class {{Entidade}}PanacheRepository implements PanacheRepository<{{Entidade}}Entity> {
}
```

## adapter/out/persistence/{{Entidade}}PersistenceAdapter.java

The outbound adapter: it implements the `{{Entidade}}Repository` port with JPA, converting
between the domain and the entity on every call. A {{Entidade}} with no `id` is new and goes to
`persist`; one with an `id` already exists, and `merge` copies what changed to the table row.
Swapping the database for something else (an HTTP service, a file) means writing another adapter
like this one, without touching the core. That is the gain that pays for the extra files.

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
    public List<{{Entidade}}> findAll() {
        return panache.listAll().stream().map({{Entidade}}Entity::toDomain).toList();
    }

    @Override
    public Optional<{{Entidade}}> findById(Long id) {
        return panache.findByIdOptional(id).map({{Entidade}}Entity::toDomain);
    }

    @Override
    public {{Entidade}} save({{Entidade}} {{entidade}}) {
        {{Entidade}}Entity entity = new {{Entidade}}Entity({{entidade}});
        if ({{entidade}}.getId() == null) {
            panache.persist(entity);
            return entity.toDomain();
        }
        return panache.getEntityManager().merge(entity).toDomain();
    }

    @Override
    public void deleteById(Long id) {
        panache.deleteById(id);
    }
}
```

## adapter/in/web/{{Entidade}}Response.java

What the API returns. Keeping the response apart from the domain makes the API contract your
decision: changing the {{Entidade}} no longer breaks the clients.

```java
package {{pacote}}.adapter.in.web;

import {{pacote}}.domain.{{Entidade}};

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

record {{Entidade}}Response(Long id{{#campos}}, {{tipo}} {{campo}}{{/campos}}) {

    static {{Entidade}}Response of({{Entidade}} {{entidade}}) {
        return new {{Entidade}}Response({{entidade}}.getId(){{#campos}}, {{entidade}}.get{{Campo}}(){{/campos}});
    }
}
```

## adapter/in/web/{{Entidade}}Resource.java

The inbound adapter: it translates HTTP to the `{{Entidade}}UseCase` port and back. It depends on
the port, not on the service. This is where the domain error becomes a 404, in a
`@ServerExceptionMapper` that only applies to this resource, because HTTP status is the web's
business. The success statuses live here too: 201 on create; a `void` method already answers
204. The class is `public` because Quarkus REST only publishes endpoints on public methods.

```java
package {{pacote}}.adapter.in.web;

import {{pacote}}.domain.{{Entidade}}NotFoundException;
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
    public List<{{Entidade}}Response> list() {
        return useCase.list().stream().map({{Entidade}}Response::of).toList();
    }

    @GET
    @Path("/{id}")
    public {{Entidade}}Response find(Long id) {
        return {{Entidade}}Response.of(useCase.find(id));
    }

    @POST
    @ResponseStatus(201)
    public {{Entidade}}Response create(@Valid {{Entidade}}Command command) {
        return {{Entidade}}Response.of(useCase.create(command));
    }

    @PUT
    @Path("/{id}")
    public {{Entidade}}Response update(Long id, @Valid {{Entidade}}Command command) {
        return {{Entidade}}Response.of(useCase.update(id, command));
    }

    @DELETE
    @Path("/{id}")
    public void delete(Long id) {
        useCase.delete(id);
    }

    @ServerExceptionMapper
    RestResponse<Problem> notFound({{Entidade}}NotFoundException e) {
        Problem problem = new Problem("Not found", Status.NOT_FOUND.getStatusCode(), e.getMessage());
        return RestResponse.ResponseBuilder.create(Status.NOT_FOUND, problem).type("application/problem+json").build();
    }

    /** The RFC 9457 error format. */
    record Problem(String title, int status, String detail) {
    }
}
```

## adapter/in/web/ErrorHandler.java

A single place for the API's errors, created once per project, in the web adapter: an error in
HTTP format is its business. Each `@ServerExceptionMapper` method turns an exception type into a
response; this one answers, in the RFC 9457 format (`application/problem+json`), the 404s that do
not come from the domain, such as a path that does not exist. Validation errors already come out
as 400 with the list of fields that failed, thanks to the `quarkus-hibernate-validator`
extension.

```java
package {{pacote}}.adapter.in.web;

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
