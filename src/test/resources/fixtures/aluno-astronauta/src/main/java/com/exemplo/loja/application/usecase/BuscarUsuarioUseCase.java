package com.exemplo.loja.application.usecase;

import com.exemplo.loja.domain.model.Usuario;
import com.exemplo.loja.domain.port.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BuscarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarios;

    public BuscarUsuarioUseCase(UsuarioRepositoryPort usuarios) {
        this.usuarios = usuarios;
    }

    public Optional<Usuario> executar(Long id) {
        return usuarios.buscarPorId(id);
    }
}
