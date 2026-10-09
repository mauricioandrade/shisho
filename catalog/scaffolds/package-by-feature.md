---
id: package-by-feature
dependencias: ["spring-boot-starter-webmvc|spring-boot-starter-web", spring-boot-starter-validation, spring-boot-starter-data-jpa]
uma_vez_por_projeto:
  TratadorDeErros.java: RestControllerAdvice
---

## {{feature}}/{{Entidade}}.java

A entidade JPA: a tabela do banco vista como objeto. Como todas as classes de `{{feature}}`, ela
é package-private (sem `public`): nenhuma outra feature enxerga a entidade, e as features
conversam pelo service. É a fronteira mais barata que existe, sem framework nenhum. Não há
setters: toda mudança passa por `atualizar()`. A tabela tem nome próprio (`{{tabela}}`) para não
esbarrar em palavras reservadas do SQL, como `user` e `order`.

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

O acesso ao banco, também package-private: só o service de `{{feature}}` lê essas tabelas. Uma
feature que precisa de dados de outra pede ao service dela. Estender `JpaRepository` já dá
`findAll`, `findById`, `save` e `delete`.

```java
package {{pacote}}.{{feature}};

import org.springframework.data.jpa.repository.JpaRepository;

interface {{Entidade}}Repository extends JpaRepository<{{Entidade}}, Long> {
}
```

## {{feature}}/{{Entidade}}Request.java

O que o cliente envia para criar ou atualizar. É um record, e não a entidade: o cliente não
consegue mandar um `id` nem campos internos. As anotações de validação só valem porque o
controller usa `@Valid`.

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

O erro de buscar um {{Entidade}} que não existe. Estender `ErrorResponseException` faz o Spring
responder 404 com um ProblemDetail (RFC 9457), sem nenhum código a mais.

```java
package {{pacote}}.{{feature}};

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

class {{Entidade}}InexistenteException extends ErrorResponseException {

    {{Entidade}}InexistenteException(Long id) {
        super(HttpStatus.NOT_FOUND);
        getBody().setDetail("{{Entidade}} " + id + " não existe");
    }
}
```

## {{feature}}/{{Entidade}}Service.java

Onde mora a regra de negócio e onde começa a transação. A classe inteira é `readOnly = true`, e
só os métodos que escrevem abrem uma transação de escrita. Repare que `atualizar` não chama
`save`: dentro da transação, o Hibernate grava sozinho o que mudou. Quando outra feature precisar
de um {{Entidade}}, é este service que ela usa. Nesse dia, torne `public` só a classe e os métodos
que ela chamar.

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

    List<{{Entidade}}Response> listar() {
        return repository.findAll().stream().map({{Entidade}}Response::de).toList();
    }

    {{Entidade}}Response buscar(Long id) {
        return {{Entidade}}Response.de(encontrar(id));
    }

    @Transactional
    {{Entidade}}Response criar({{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = new {{Entidade}}({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        return {{Entidade}}Response.de(repository.save({{entidade}}));
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
        return repository.findById(id).orElseThrow(() -> new {{Entidade}}InexistenteException(id));
    }
}
```

## {{feature}}/{{Entidade}}Controller.java

A porta HTTP. O controller só traduz: lê a requisição, chama o service e escolhe o status da
resposta (201 ao criar, 204 ao remover). A dependência chega pelo construtor e é `final`, e o
teste instancia a classe sem Spring.

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
    List<{{Entidade}}Response> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    {{Entidade}}Response buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    {{Entidade}}Response criar(@Valid @RequestBody {{Entidade}}Request request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    {{Entidade}}Response atualizar(@PathVariable Long id, @Valid @RequestBody {{Entidade}}Request request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
```

## TratadorDeErros.java

Um lugar só para os erros da API, criado uma vez por projeto, no pacote base: ele vale para
todas as features. Herdar de `ResponseEntityExceptionHandler` faz o Spring responder no formato
ProblemDetail (RFC 9457) a erros de validação, JSON malformado e exceções como
{{Entidade}}InexistenteException. Um erro novo do domínio ganha um `@ExceptionHandler` aqui.

```java
package {{pacote}};

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class TratadorDeErros extends ResponseEntityExceptionHandler {
}
```
