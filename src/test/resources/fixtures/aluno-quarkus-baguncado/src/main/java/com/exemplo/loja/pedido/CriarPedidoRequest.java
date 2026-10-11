package com.exemplo.loja.pedido;

import jakarta.validation.constraints.NotBlank;

public class CriarPedidoRequest {

    @NotBlank
    public String cliente;

    public long totalEmCentavos;
}
