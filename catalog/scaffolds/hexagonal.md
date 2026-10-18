---
id: hexagonal
dependencias: ["spring-boot-starter-webmvc|spring-boot-starter-web", spring-boot-starter-validation, spring-boot-starter-data-jpa]
recusa_imports: [javax.persistence, javax.validation, javax.servlet]
plataforma_minima: ["org.springframework.boot:3"]
uma_vez_por_projeto:
  adapter/in/web/TratadorDeErros.java: RestControllerAdvice
---

## domain/{{Entidade}}.java

O coração do hexágono: um {{Entidade}} em Java puro, sem JPA e sem Spring. É o que permite testar
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
`{{Entidade}}Controller`.

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

A porta de entrada: o que o mundo de fora pode pedir ao núcleo. O controller depende desta
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

A porta de saída: o que o núcleo precisa do mundo de fora, nos termos dele. Não há `JpaRepository`
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
acompanha. A classe é package-private: ninguém a usa diretamente, só pela porta. O `@Service` e o
`@Transactional` são Spring no núcleo, uma concessão: tirá-los pediria uma classe de configuração
com `@Bean` para cada service.

```java
package {{pacote}}.application;

import {{pacote}}.domain.{{Entidade}};
import {{pacote}}.domain.{{Entidade}}InexistenteException;
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

## adapter/out/persistence/{{Entidade}}JpaRepository.java

O Spring Data, escondido dentro do adaptador. Ele trabalha com a `{{Entidade}}Entity`, nunca com
o domínio.

```java
package {{pacote}}.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface {{Entidade}}JpaRepository extends JpaRepository<{{Entidade}}Entity, Long> {
}
```

## adapter/out/persistence/{{Entidade}}PersistenceAdapter.java

O adaptador de saída: implementa a porta `{{Entidade}}Repository` com JPA, convertendo entre o
domínio e a entidade em cada chamada. Trocar o banco por outra coisa (um serviço HTTP, um
arquivo) é escrever outro adaptador como este, sem mexer no núcleo. É esse o ganho que paga os
arquivos a mais.

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
    public List<{{Entidade}}> todos() {
        return jpa.findAll().stream().map({{Entidade}}Entity::paraDominio).toList();
    }

    @Override
    public Optional<{{Entidade}}> porId(Long id) {
        return jpa.findById(id).map({{Entidade}}Entity::paraDominio);
    }

    @Override
    public {{Entidade}} salvar({{Entidade}} {{entidade}}) {
        return jpa.save(new {{Entidade}}Entity({{entidade}})).paraDominio();
    }

    @Override
    public void remover(Long id) {
        jpa.deleteById(id);
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

## adapter/in/web/{{Entidade}}Controller.java

O adaptador de entrada: traduz HTTP para a porta `{{Entidade}}UseCase` e de volta. Depende da
porta, e não do service. É aqui que o erro do domínio vira 404, no `@ExceptionHandler`, porque
status HTTP é assunto da web. Os status de sucesso também ficam aqui: 201 ao criar, 204 ao
remover.

```java
package {{pacote}}.adapter.in.web;

import {{pacote}}.domain.{{Entidade}}InexistenteException;
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
    List<{{Entidade}}Response> listar() {
        return useCase.listar().stream().map({{Entidade}}Response::de).toList();
    }

    @GetMapping("/{id}")
    {{Entidade}}Response buscar(@PathVariable Long id) {
        return {{Entidade}}Response.de(useCase.buscar(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    {{Entidade}}Response criar(@Valid @RequestBody {{Entidade}}Command command) {
        return {{Entidade}}Response.de(useCase.criar(command));
    }

    @PutMapping("/{id}")
    {{Entidade}}Response atualizar(@PathVariable Long id, @Valid @RequestBody {{Entidade}}Command command) {
        return {{Entidade}}Response.de(useCase.atualizar(id, command));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remover(@PathVariable Long id) {
        useCase.remover(id);
    }

    @ExceptionHandler({{Entidade}}InexistenteException.class)
    ProblemDetail inexistente({{Entidade}}InexistenteException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }
}
```

## adapter/in/web/TratadorDeErros.java

Um lugar só para os erros da API, criado uma vez por projeto, no adaptador web: erro em formato
HTTP é assunto dele. Herdar de `ResponseEntityExceptionHandler` faz o Spring responder no formato
ProblemDetail (RFC 9457) a erros de validação e JSON malformado. Nos erros de validação, a
resposta ganha um campo `campos` dizendo o que falhou em cada campo.

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
class TratadorDeErros extends ResponseEntityExceptionHandler {

    /** Diz ao cliente quais campos falharam, e não só que a requisição é inválida. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> campos = new TreeMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ProblemDetail problema = e.getBody();
        problema.setProperty("campos", campos);
        return handleExceptionInternal(e, problema, headers, status, request);
    }
}
```
