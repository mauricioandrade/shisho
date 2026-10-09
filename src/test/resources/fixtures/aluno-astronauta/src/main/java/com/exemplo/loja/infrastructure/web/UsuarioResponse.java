package com.exemplo.loja.infrastructure.web;

import com.exemplo.loja.domain.model.Usuario;

record UsuarioResponse(Long id, String nome) {

    static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.id(), usuario.nome());
    }
}
