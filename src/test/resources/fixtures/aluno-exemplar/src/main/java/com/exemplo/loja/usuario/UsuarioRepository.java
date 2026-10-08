package com.exemplo.loja.usuario;

interface UsuarioRepository {

    long count();

    Usuario save(Usuario usuario);
}
