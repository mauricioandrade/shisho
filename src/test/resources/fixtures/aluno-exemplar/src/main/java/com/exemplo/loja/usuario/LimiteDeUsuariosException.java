package com.exemplo.loja.usuario;

class LimiteDeUsuariosException extends RuntimeException {

    LimiteDeUsuariosException(int limite) {
        super("limite de " + limite + " usuarios atingido");
    }
}
