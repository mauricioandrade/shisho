package com.exemplo.loja.usuario;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
class UsuarioService {

    @Inject
    UsuarioRepository usuarios;

    List<UsuarioResponse> listar() {
        return usuarios.listAll().stream().map(UsuarioResponse::de).toList();
    }

    @Transactional
    UsuarioResponse criar(CriarUsuarioRequest pedido) {
        Usuario usuario = new Usuario(pedido.nome(), pedido.email());
        usuarios.persist(usuario);
        return UsuarioResponse.de(usuario);
    }
}
