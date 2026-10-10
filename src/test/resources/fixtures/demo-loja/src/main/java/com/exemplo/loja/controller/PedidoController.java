package com.exemplo.loja.controller;

import com.exemplo.loja.dto.PedidoRequest;
import com.exemplo.loja.model.Pedido;
import com.exemplo.loja.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    @Autowired
    private PedidoService service;

    @GetMapping
    public List<Pedido> listar() {
        return service.listar();
    }

    @PostMapping
    public void criar(@RequestBody PedidoRequest request) {
        service.criar(request);
    }
}
