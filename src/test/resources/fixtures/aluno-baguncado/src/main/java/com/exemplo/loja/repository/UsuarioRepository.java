package com.exemplo.loja.repository;

import com.exemplo.loja.model.Usuario;

import java.util.List;

public interface UsuarioRepository {

    void salvar(String nome, int limite);

    List<Usuario> listar();
}
