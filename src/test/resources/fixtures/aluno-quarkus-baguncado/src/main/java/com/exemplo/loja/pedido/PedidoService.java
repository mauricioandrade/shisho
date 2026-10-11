package com.exemplo.loja.pedido;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
class PedidoService {

    private FreteService frete;

    PedidoService(FreteService frete) {
        this.frete = frete;
    }

    Pedido buscar(Long id) {
        return Pedido.findById(id);
    }

    @Transactional
    Pedido criar(CriarPedidoRequest pedido) {
        Pedido novo = new Pedido();
        novo.cliente = pedido.cliente;
        novo.totalEmCentavos = pedido.totalEmCentavos + frete.calcular(pedido.totalEmCentavos, 1);
        novo.persist();
        System.out.println("Pedido criado: " + novo.id);
        return novo;
    }
}
