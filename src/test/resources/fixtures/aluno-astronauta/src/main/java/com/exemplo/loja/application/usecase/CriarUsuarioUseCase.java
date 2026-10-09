package com.exemplo.loja.application.usecase;

import com.exemplo.loja.domain.model.Usuario;
import com.exemplo.loja.domain.port.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class CriarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarios;

    public CriarUsuarioUseCase(UsuarioRepositoryPort usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario executar(String nome) {
        return usuarios.salvar(new Usuario(null, nome));
    }
}
