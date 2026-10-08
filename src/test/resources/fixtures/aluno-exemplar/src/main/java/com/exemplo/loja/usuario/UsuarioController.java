package com.exemplo.loja.usuario;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/usuarios")
class UsuarioController {

    private static final Logger log = LoggerFactory.getLogger(UsuarioController.class);

    private final UsuarioService usuarioService;

    UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    UsuarioResponse criar(@Valid @RequestBody CriarUsuarioRequest request) {
        log.info("criando usuario {}", request.nome());
        return UsuarioResponse.de(usuarioService.criar(request.nome()));
    }

    @PostMapping("/webhook")
    void webhook(@RequestBody String payload) {
        log.debug("webhook recebido: {} bytes", payload.length());
    }
}
