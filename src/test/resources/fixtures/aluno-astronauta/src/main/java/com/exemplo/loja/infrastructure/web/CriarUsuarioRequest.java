package com.exemplo.loja.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

record CriarUsuarioRequest(@NotBlank String nome) {
}
