package com.exemplo.loja.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CriarUsuarioRequest(@NotBlank String nome, @Email String email) {
}
