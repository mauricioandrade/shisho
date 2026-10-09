package com.exemplo.loja.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioJpaRepository extends JpaRepository<UsuarioJpaEntity, Long> {
}
