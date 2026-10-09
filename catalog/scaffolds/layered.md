---
id: layered
dependencias: ["spring-boot-starter-webmvc|spring-boot-starter-web", spring-boot-starter-validation, spring-boot-starter-data-jpa]
uma_vez_por_projeto:
  controller/TratadorDeErros.java: RestControllerAdvice
---

## model/{{Entidade}}.java

A entidade JPA: a tabela do banco vista como objeto. O construtor sem argumentos é `protected`
porque só o Hibernate deve usá-lo; quem cria um {{Entidade}} passa pelo construtor com os campos.
A tabela tem nome próprio (`{{tabela}}`) para não esbarrar em palavras reservadas do SQL, como
`user` e `order`.
Não há setters: toda mudança passa por `atualizar()`, e é ali que uma regra de negócio entra
quando aparecer.

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

## repository/{{Entidade}}Repository.java

O acesso ao banco. Estender `JpaRepository` já dá `findAll`, `findById`, `save` e `delete`, sem
uma linha de SQL. Consultas novas viram métodos aqui, como `findByNome(String nome)`, e o Spring
Data escreve a consulta a partir do nome.

```java
package {{pacote}}.repository;

import {{pacote}}.model.{{Entidade}};
import org.springframework.data.jpa.repository.JpaRepository;

public interface {{Entidade}}Repository extends JpaRepository<{{Entidade}}, Long> {
}
```

## dto/{{Entidade}}Request.java

O que o cliente envia para criar ou atualizar. É um record, imutável e sem código repetido, e
não a entidade: assim o cliente não consegue mandar um `id` nem campos internos. As anotações de
validação só valem porque o controller usa `@Valid`.

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

O que a API devolve. Separar a resposta da entidade faz do contrato da API uma decisão sua, e
não um reflexo do esquema do banco. Mudar uma coluna deixa de quebrar os clientes.

```java
package {{pacote}}.dto;

import {{pacote}}.model.{{Entidade}};

{{#importacoes}}
import {{importacao}};
{{/importacoes}}

public record {{Entidade}}Response(Long id{{#campos}}, {{tipo}} {{campo}}{{/campos}}) {

    public static {{Entidade}}Response de({{Entidade}} {{entidade}}) {
        return new {{Entidade}}Response({{entidade}}.getId(){{#campos}}, {{entidade}}.get{{Campo}}(){{/campos}});
    }
}
```

## service/{{Entidade}}InexistenteException.java

O erro de buscar um {{Entidade}} que não existe. Estender `ErrorResponseException` faz o Spring
responder 404 com um ProblemDetail (RFC 9457), sem nenhum código a mais. O custo é o service
conhecer um status HTTP. Se outro canal (uma fila, um job) passar a usar o service, troque por
uma exceção sem HTTP e trate-a no TratadorDeErros.

```java
package {{pacote}}.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

public class {{Entidade}}InexistenteException extends ErrorResponseException {

    public {{Entidade}}InexistenteException(Long id) {
        super(HttpStatus.NOT_FOUND);
        getBody().setDetail("{{Entidade}} " + id + " não existe");
    }
}
```

## service/{{Entidade}}Service.java

Onde mora a regra de negócio e onde começa a transação. A classe inteira é `readOnly = true`, e
só os métodos que escrevem abrem uma transação de escrita: leituras não pagam o dirty checking
do Hibernate. Repare que `atualizar` não chama `save`: dentro da transação, o Hibernate grava
sozinho o que mudou na entidade.

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

    public List<{{Entidade}}Response> listar() {
        return repository.findAll().stream().map({{Entidade}}Response::de).toList();
    }

    public {{Entidade}}Response buscar(Long id) {
        return {{Entidade}}Response.de(encontrar(id));
    }

    @Transactional
    public {{Entidade}}Response criar({{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = new {{Entidade}}({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        return {{Entidade}}Response.de(repository.save({{entidade}}));
    }

    @Transactional
    public {{Entidade}}Response atualizar(Long id, {{Entidade}}Request request) {
        {{Entidade}} {{entidade}} = encontrar(id);
        {{entidade}}.atualizar({{#campos}}request.{{campo}}(){{,}}{{/campos}});
        return {{Entidade}}Response.de({{entidade}});
    }

    @Transactional
    public void remover(Long id) {
        repository.delete(encontrar(id));
    }

    private {{Entidade}} encontrar(Long id) {
        return repository.findById(id).orElseThrow(() -> new {{Entidade}}InexistenteException(id));
    }
}
```

## controller/{{Entidade}}Controller.java

A porta HTTP. O controller só traduz: lê a requisição, chama o service e escolhe o status da
resposta (201 ao criar, 204 ao remover). Regra de negócio aqui é sinal de que ela fugiu do
service. A dependência chega pelo construtor e é `final`: fica visível do que a classe precisa, e
o teste a instancia sem Spring.

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
    public List<{{Entidade}}Response> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public {{Entidade}}Response buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public {{Entidade}}Response criar(@Valid @RequestBody {{Entidade}}Request request) {
        return service.criar(request);
    }

    @PutMapping("/{id}")
    public {{Entidade}}Response atualizar(@PathVariable Long id, @Valid @RequestBody {{Entidade}}Request request) {
        return service.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@PathVariable Long id) {
        service.remover(id);
    }
}
```

## controller/TratadorDeErros.java

Um lugar só para os erros da API, criado uma vez por projeto. Herdar de
`ResponseEntityExceptionHandler` faz o Spring responder no formato ProblemDetail (RFC 9457) a
erros de validação, JSON malformado e exceções como {{Entidade}}InexistenteException. Um erro novo
do domínio ganha um `@ExceptionHandler` aqui, e não um try/catch em cada endpoint.

```java
package {{pacote}}.controller;

import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class TratadorDeErros extends ResponseEntityExceptionHandler {
}
```
