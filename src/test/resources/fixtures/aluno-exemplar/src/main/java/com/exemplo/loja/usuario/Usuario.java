package com.exemplo.loja.usuario;

class Usuario {

    private Long id;
    private String nome;

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
