package com.exemplo.loja.service;

import com.exemplo.loja.dto.UsuarioRequest;
import com.exemplo.loja.model.Usuario;
import com.exemplo.loja.repository.UsuarioRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UsuarioService {

    private UsuarioRepository usuarioRepository;

    @Value("${loja.limite-usuarios}")
    private int limite;

    public void criar(UsuarioRequest request) {
        try {
            usuarioRepository.salvar(request.getNome(), limite);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Usuario> listar() {
        return usuarioRepository.listar();
    }
}
