package com.exemplo.loja.infrastructure.persistence;

import com.exemplo.loja.domain.model.Usuario;

final class UsuarioMapper {

    private UsuarioMapper() {
    }

    static Usuario toDomain(UsuarioJpaEntity entity) {
        return new Usuario(entity.getId(), entity.getNome());
    }

    static UsuarioJpaEntity toEntity(Usuario usuario) {
        return new UsuarioJpaEntity(usuario.id(), usuario.nome());
    }
}
