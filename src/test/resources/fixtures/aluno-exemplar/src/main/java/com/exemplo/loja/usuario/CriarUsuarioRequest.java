package com.exemplo.loja.usuario;

import jakarta.validation.constraints.NotBlank;

record CriarUsuarioRequest(@NotBlank String nome) {
}
