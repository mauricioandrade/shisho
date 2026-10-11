package com.exemplo.loja.usuario;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class UsuarioRepository implements PanacheRepository<Usuario> {
}
