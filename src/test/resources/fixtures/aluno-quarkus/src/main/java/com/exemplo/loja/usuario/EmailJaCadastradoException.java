package com.exemplo.loja.usuario;

class EmailJaCadastradoException extends RuntimeException {

    EmailJaCadastradoException(String email) {
        super("E-mail já cadastrado: " + email);
    }
}
