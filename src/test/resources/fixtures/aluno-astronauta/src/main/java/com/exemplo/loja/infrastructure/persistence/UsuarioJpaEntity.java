package com.exemplo.loja.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
class UsuarioJpaEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String nome;

    protected UsuarioJpaEntity() {
    }

    UsuarioJpaEntity(Long id, String nome) {
        this.id = id;
        this.nome = nome;
    }

    Long getId() {
        return id;
    }

    String getNome() {
        return nome;
    }
}
