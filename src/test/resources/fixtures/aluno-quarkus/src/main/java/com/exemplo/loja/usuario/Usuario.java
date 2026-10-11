package com.exemplo.loja.usuario;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Usuario {

    @Id
    @GeneratedValue
    private Long id;

    private String nome;

    private String email;

    protected Usuario() {
    }

    Usuario(String nome, String email) {
        this.nome = nome;
        this.email = email;
    }

    Long getId() {
        return id;
    }

    String getNome() {
        return nome;
    }

    String getEmail() {
        return email;
    }
}
