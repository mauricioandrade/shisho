package com.exemplo.loja.usuario;

import java.util.List;

interface UsuarioRepository {

    long count();

    List<Usuario> findAll();

    Usuario save(Usuario usuario);
}
