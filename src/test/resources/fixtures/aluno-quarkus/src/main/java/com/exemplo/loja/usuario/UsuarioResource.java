package com.exemplo.loja.usuario;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import java.util.List;

@Path("/usuarios")
public class UsuarioResource {

    @Inject
    UsuarioService service;

    @GET
    public List<UsuarioResponse> listar() {
        return service.listar();
    }

    @POST
    public UsuarioResponse criar(@Valid CriarUsuarioRequest pedido) {
        return service.criar(pedido);
    }
}
