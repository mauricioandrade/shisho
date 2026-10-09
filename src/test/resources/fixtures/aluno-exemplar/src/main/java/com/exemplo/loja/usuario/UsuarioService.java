package com.exemplo.loja.usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final int limite;

    UsuarioService(UsuarioRepository usuarioRepository, @Value("${loja.limite-usuarios}") int limite) {
        this.usuarioRepository = usuarioRepository;
        this.limite = limite;
    }

    List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    @Transactional
    Usuario criar(String nome) {
        if (usuarioRepository.count() >= limite) {
            throw new LimiteDeUsuariosException(limite);
        }
        return usuarioRepository.save(new Usuario(nome));
    }
}
