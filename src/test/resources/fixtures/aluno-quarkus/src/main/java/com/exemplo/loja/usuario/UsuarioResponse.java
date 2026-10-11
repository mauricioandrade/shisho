package com.exemplo.loja.usuario;

public record UsuarioResponse(Long id, String nome, String email) {

    static UsuarioResponse de(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
