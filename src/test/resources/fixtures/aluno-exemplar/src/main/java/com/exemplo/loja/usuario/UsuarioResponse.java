package com.exemplo.loja.usuario;

record UsuarioResponse(Long id, String nome) {

    static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome());
    }
}
