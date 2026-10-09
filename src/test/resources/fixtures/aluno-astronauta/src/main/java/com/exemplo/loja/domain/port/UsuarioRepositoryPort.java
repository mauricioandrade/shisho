package com.exemplo.loja.domain.port;

import com.exemplo.loja.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepositoryPort {

    Usuario salvar(Usuario usuario);

    Optional<Usuario> buscarPorId(Long id);

    List<Usuario> listar();

    void remover(Long id);
}
