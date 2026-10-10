package com.exemplo.loja.dto;

import jakarta.validation.constraints.NotBlank;

public record PedidoRequest(@NotBlank String cliente) {
}
