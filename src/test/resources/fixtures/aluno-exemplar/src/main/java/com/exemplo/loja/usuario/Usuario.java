package com.exemplo.loja.usuario;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
class Usuario {

    @Id
    @GeneratedValue
    private Long id;
    private String nome;

    protected Usuario() {
    }

    Usuario(String nome) {
        this.nome = nome;
    }

    Long getId() {
        return id;
    }

    String getNome() {
        return nome;
    }

    void renomear(String nome) {
        this.nome = nome;
    }
}
