---
id: hexagonal
uma_vez_por_projeto:
  adapter/in/web/ErrorHandler.java: RestControllerAdvice
---

## domain/{{Entidade}}.java

The heart of the hexagon: a {{Entidade}} in plain Java, with no JPA and no Spring. That is what
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
only through the port. `@Service` and `@Transactional` are Spring inside the core, a concession:
removing them would take a configuration class with a `@Bean` for every service.

```java
package {{pacote}}.application;

import {{pacote}}.domain.{{Entidade}};
import {{pacote}}.domain.{{Entidade}}NotFoundException;
import {{pacote}}.port.in.{{Entidade}}Command;
import {{pacote}}.port.in.{{Entidade}}UseCase;
import {{pacote}}.port.out.{{Entidade}}Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
class {{Entidade}}Service implements {{Entidade}}UseCase {

    private final {{Entidade}}Repository repository;

    {{Entidade}}Service({{Entidade}}Repository repository) {
        this.repository = repository;
    }

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

## adapter/out/persistence/{{Entidade}}JpaRepository.java

Spring Data, hidden inside the adapter. It works with `{{Entidade}}Entity`, never with the domain.

```java
package {{pacote}}.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface {{Entidade}}JpaRepository extends JpaRepository<{{Entidade}}Entity, Long> {
}
```

## adapter/out/persistence/{{Entidade}}PersistenceAdapter.java

The outbound adapter: it implements the `{{Entidade}}Repository` port with JPA, converting
between the domain and the entity on every call. Replacing the database with something else (an
HTTP service, a file) means writing another adapter like this one, without touching the core.
That is the gain that pays for the extra files.

```java
package {{pacote}}.adapter.out.persistence;

import {{pacote}}.domain.{{Entidade}};
import {{pacote}}.port.out.{{Entidade}}Repository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
class {{Entidade}}PersistenceAdapter implements {{Entidade}}Repository {

    private final {{Entidade}}JpaRepository jpa;

    {{Entidade}}PersistenceAdapter({{Entidade}}JpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<{{Entidade}}> findAll() {
        return jpa.findAll().stream().map({{Entidade}}Entity::toDomain).toList();
    }

    @Override
    public Optional<{{Entidade}}> findById(Long id) {
        return jpa.findById(id).map({{Entidade}}Entity::toDomain);
    }

    @Override
    public {{Entidade}} save({{Entidade}} {{entidade}}) {
        return jpa.save(new {{Entidade}}Entity({{entidade}})).toDomain();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
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

## adapter/in/web/{{Entidade}}Controller.java

The inbound adapter: it translates HTTP into the `{{Entidade}}UseCase` port and back. It depends
on the port, not on the service. This is where the domain error becomes a 404, in the
`@ExceptionHandler`, because HTTP statuses are the web's business. The success statuses live here
too: 201 on create, 204 on delete.

```java
package {{pacote}}.adapter.in.web;

import {{pacote}}.domain.{{Entidade}}NotFoundException;
import {{pacote}}.port.in.{{Entidade}}Command;
import {{pacote}}.port.in.{{Entidade}}UseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/{{rota}}")
class {{Entidade}}Controller {

    private final {{Entidade}}UseCase useCase;

    {{Entidade}}Controller({{Entidade}}UseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    List<{{Entidade}}Response> list() {
        return useCase.list().stream().map({{Entidade}}Response::of).toList();
    }

    @GetMapping("/{id}")
    {{Entidade}}Response find(@PathVariable Long id) {
        return {{Entidade}}Response.of(useCase.find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    {{Entidade}}Response create(@Valid @RequestBody {{Entidade}}Command command) {
        return {{Entidade}}Response.of(useCase.create(command));
    }

    @PutMapping("/{id}")
    {{Entidade}}Response update(@PathVariable Long id, @Valid @RequestBody {{Entidade}}Command command) {
        return {{Entidade}}Response.of(useCase.update(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        useCase.delete(id);
    }

    @ExceptionHandler({{Entidade}}NotFoundException.class)
    ProblemDetail notFound({{Entidade}}NotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
```

## adapter/in/web/ErrorHandler.java

A single place for the API's errors, created once per project, in the web adapter: an error in
HTTP format is its business. Extending `ResponseEntityExceptionHandler` makes Spring answer in the
ProblemDetail format (RFC 9457) to validation errors and malformed JSON. On validation errors, the
response gains a `fields` property saying what failed in each field.

```java
package {{pacote}}.adapter.in.web;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;

@RestControllerAdvice
class ErrorHandler extends ResponseEntityExceptionHandler {

    /** Tells the client which fields failed, not only that the request is invalid. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new TreeMap<>();
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fields.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problem = e.getBody();
        problem.setProperty("fields", fields);
        return handleExceptionInternal(e, problem, headers, status, request);
    }
}
```
