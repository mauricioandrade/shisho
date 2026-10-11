package com.exemplo.loja.pedido;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/pedidos")
public class PedidoResource {

    @Inject
    PedidoService service;

    @GET
    @Path("{id}")
    public Pedido buscar(Long id) {
        return service.buscar(id);
    }

    @POST
    public Pedido criar(CriarPedidoRequest pedido) {
        return service.criar(pedido);
    }
}
