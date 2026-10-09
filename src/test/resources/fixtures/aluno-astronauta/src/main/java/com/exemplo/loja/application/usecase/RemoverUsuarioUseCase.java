package com.exemplo.loja.application.usecase;

import com.exemplo.loja.domain.port.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class RemoverUsuarioUseCase {

    private final UsuarioRepositoryPort usuarios;

    public RemoverUsuarioUseCase(UsuarioRepositoryPort usuarios) {
        this.usuarios = usuarios;
    }

    public void executar(Long id) {
        usuarios.remover(id);
    }
}
