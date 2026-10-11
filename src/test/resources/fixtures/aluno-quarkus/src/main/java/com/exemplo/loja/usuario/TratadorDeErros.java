package com.exemplo.loja.usuario;

import jakarta.ws.rs.core.Response.Status;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

class TratadorDeErros {

    @ServerExceptionMapper
    RestResponse<Problema> emailJaCadastrado(EmailJaCadastradoException erro) {
        Problema problema = new Problema("E-mail já cadastrado", Status.CONFLICT.getStatusCode(), erro.getMessage());
        return RestResponse.status(Status.CONFLICT, problema);
    }
}
