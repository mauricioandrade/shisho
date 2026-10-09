package com.exemplo.loja.infrastructure.persistence;

import com.exemplo.loja.domain.model.Usuario;
import com.exemplo.loja.domain.port.UsuarioRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class UsuarioPersistenceAdapter implements UsuarioRepositoryPort {

    private final UsuarioJpaRepository repository;

    UsuarioPersistenceAdapter(UsuarioJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Usuario salvar(Usuario usuario) {
        return UsuarioMapper.toDomain(repository.save(UsuarioMapper.toEntity(usuario)));
    }

    @Override
    public Optional<Usuario> buscarPorId(Long id) {
        return repository.findById(id).map(UsuarioMapper::toDomain);
    }

    @Override
    public List<Usuario> listar() {
        return repository.findAll().stream().map(UsuarioMapper::toDomain).toList();
    }

    @Override
    public void remover(Long id) {
        repository.deleteById(id);
    }
}
