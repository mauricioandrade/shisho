package com.exemplo.loja.application.usecase;

import com.exemplo.loja.domain.model.Usuario;
import com.exemplo.loja.domain.port.UsuarioRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListarUsuariosUseCase {

    private final UsuarioRepositoryPort usuarios;

    public ListarUsuariosUseCase(UsuarioRepositoryPort usuarios) {
        this.usuarios = usuarios;
    }

    public List<Usuario> executar() {
        return usuarios.listar();
    }
}
