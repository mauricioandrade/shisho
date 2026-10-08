package com.exemplo.loja.controller;

import com.exemplo.loja.dto.UsuarioRequest;
import com.exemplo.loja.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    public void criar(@RequestBody UsuarioRequest request) {
        System.out.println("criando usuario " + request.getNome());
        usuarioService.criar(request);
    }
}
