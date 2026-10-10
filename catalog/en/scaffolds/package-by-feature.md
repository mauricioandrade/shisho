---
id: package-by-feature
uma_vez_por_projeto:
  ErrorHandler.java: RestControllerAdvice
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

Database access, package-private too: only the `{{feature}}` service reads these tables. A
feature that needs another one's data asks that feature's service. Extending `JpaRepository`
already gives you `findAll`, `findById`, `save` and `delete`.

```java
package {{pacote}}.{{feature}};

import org.springframework.data.jpa.repository.JpaRepository;

interface {{Entidade}}Repository extends JpaRepository<{{Entidade}}, Long> {
}
```

## {{feature}}/{{Entidade}}Request.java

What the client sends to create or update. It is a record, not the entity: the client cannot
send an `id` or internal fields. The validation annotations only count because the controller
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

The error of looking up a {{Entidade}} that does not exist. Extending `ErrorResponseException`
makes Spring answer 404 with a ProblemDetail (RFC 9457), with no extra code.

```java
package {{pacote}}.{{feature}};

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

class {{Entidade}}NotFoundException extends ErrorResponseException {

    {{Entidade}}NotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND);
        getBody().setDetail("{{Entidade}} " + id + " does not exist");
    }
}
```

## {{feature}}/{{Entidade}}Service.java

Where the business rules live and where the transaction starts. The whole class is
`readOnly = true`, and only the methods that write open a write transaction. Notice that `update`
does not call `save`: inside the transaction, Hibernate writes whatever changed by itself. When
another feature needs a {{Entidade}}, this is the service it uses. On that day, make `public`
only the class and the methods it calls.

```java
package {{pacote}}.{{feature}};

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
class {{Entidade}}Service {

    private final {{Entidade}}Repository repository;

    {{Entidade}}Service({{Entidade}}Repository repository) {
        this.repository = repository;
    }

    List<{{Entidade}}Response> list() {
        return repository.findAll().stream().map({{Entidade}}Response::of).toList();
    }

    {{Entidade}}Response find(Long id) {
        return {{Entidade}}Response.of(load(id));
    }

    @Transactional
    {{Entidade}}Response create({{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = new {{Entidade}}({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        return {{Entidade}}Response.of(repository.save({{entidade}}));
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
        return repository.findById(id).orElseThrow(() -> new {{Entidade}}NotFoundException(id));
    }
}
```

## {{feature}}/{{Entidade}}Controller.java

The HTTP door. The controller only translates: it reads the request, calls the service and picks
the response status (201 on create, 204 on delete). The dependency arrives through the
constructor and is `final`, and a test creates the class without Spring.

```java
package {{pacote}}.{{feature}};

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
class {{Entidade}}Controller {

    private final {{Entidade}}Service service;

    {{Entidade}}Controller({{Entidade}}Service service) {
        this.service = service;
    }

    @GetMapping
    List<{{Entidade}}Response> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    {{Entidade}}Response find(@PathVariable Long id) {
        return service.find(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    {{Entidade}}Response create(@Valid @RequestBody {{Entidade}}Request request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    {{Entidade}}Response update(@PathVariable Long id, @Valid @RequestBody {{Entidade}}Request request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
```

## ErrorHandler.java

A single place for the API's errors, created once per project, in the base package: it applies
to every feature. Extending `ResponseEntityExceptionHandler` makes Spring answer in the
ProblemDetail format (RFC 9457) to validation errors, malformed JSON and exceptions such as
{{Entidade}}NotFoundException. On validation errors, the response gains a `fields` property
saying what failed in each field. A new domain error gets an `@ExceptionHandler` here.

```java
package {{pacote}};

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
