---
id: layered
uma_vez_por_projeto:
  controller/ErrorHandler.java: RestControllerAdvice
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

Database access. Extending `JpaRepository` already gives you `findAll`, `findById`, `save` and
`delete`, without a single line of SQL. New queries become methods here, such as
`findByName(String name)`, and Spring Data writes the query from the name.

```java
package {{pacote}}.repository;

import {{pacote}}.model.{{Entidade}};
import org.springframework.data.jpa.repository.JpaRepository;

public interface {{Entidade}}Repository extends JpaRepository<{{Entidade}}, Long> {
}
```

## dto/{{Entidade}}Request.java

What the client sends to create or update. It is a record, immutable and free of boilerplate,
and not the entity: that way the client cannot send an `id` or internal fields. The validation
annotations only count because the controller uses `@Valid`.

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

The error of looking up a {{Entidade}} that does not exist. Extending `ErrorResponseException`
makes Spring answer 404 with a ProblemDetail (RFC 9457), with no extra code. The cost is the
service knowing an HTTP status. If another channel (a queue, a job) starts using the service,
switch to an exception with no HTTP in it and handle it in the ErrorHandler.

```java
package {{pacote}}.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class {{Entidade}}NotFoundException extends ErrorResponseException {

    public {{Entidade}}NotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND);
        getBody().setDetail("{{Entidade}} " + id + " does not exist");
    }
}
```

## service/{{Entidade}}Service.java

Where the business rules live and where the transaction starts. The whole class is
`readOnly = true`, and only the methods that write open a write transaction: reads do not pay for
Hibernate's dirty checking. Notice that `update` does not call `save`: inside the transaction,
Hibernate writes whatever changed in the entity by itself.

```java
package {{pacote}}.service;

import {{pacote}}.dto.{{Entidade}}Request;
import {{pacote}}.dto.{{Entidade}}Response;
import {{pacote}}.model.{{Entidade}};
import {{pacote}}.repository.{{Entidade}}Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class {{Entidade}}Service {

    private final {{Entidade}}Repository repository;

    public {{Entidade}}Service({{Entidade}}Repository repository) {
        this.repository = repository;
    }

    public List<{{Entidade}}Response> list() {
        return repository.findAll().stream().map({{Entidade}}Response::of).toList();
    }

    public {{Entidade}}Response find(Long id) {
        return {{Entidade}}Response.of(load(id));
    }

    @Transactional
    public {{Entidade}}Response create({{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = new {{Entidade}}({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        return {{Entidade}}Response.of(repository.save({{entidade}}));
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
        return repository.findById(id).orElseThrow(() -> new {{Entidade}}NotFoundException(id));
    }
}
```

## controller/{{Entidade}}Controller.java

The HTTP door. The controller only translates: it reads the request, calls the service and picks
the response status (201 on create, 204 on delete). A business rule here is a sign it escaped
from the service. The dependency arrives through the constructor and is `final`: what the class
needs is visible, and a test creates it without Spring.

```java
package {{pacote}}.controller;

import {{pacote}}.dto.{{Entidade}}Request;
import {{pacote}}.dto.{{Entidade}}Response;
import {{pacote}}.service.{{Entidade}}Service;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class {{Entidade}}Controller {

    private final {{Entidade}}Service service;

    public {{Entidade}}Controller({{Entidade}}Service service) {
        this.service = service;
    }

    @GetMapping
    public List<{{Entidade}}Response> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public {{Entidade}}Response find(@PathVariable Long id) {
        return service.find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public {{Entidade}}Response create(@Valid @RequestBody {{Entidade}}Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public {{Entidade}}Response update(@PathVariable Long id, @Valid @RequestBody {{Entidade}}Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
```

## controller/ErrorHandler.java

A single place for the API's errors, created once per project. Extending
`ResponseEntityExceptionHandler` makes Spring answer in the ProblemDetail format (RFC 9457) to
validation errors, malformed JSON and exceptions such as {{Entidade}}NotFoundException. On
validation errors, the response gains a `fields` property saying what failed in each field. A
new domain error gets an `@ExceptionHandler` here, not a try/catch in every endpoint.

```java
package {{pacote}}.controller;

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
public class ErrorHandler extends ResponseEntityExceptionHandler {

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
