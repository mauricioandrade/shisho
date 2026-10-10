package com.exemplo.loja.service;

import com.exemplo.loja.dto.PedidoRequest;
import com.exemplo.loja.model.Pedido;
import com.exemplo.loja.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @Transactional
    public void criar(PedidoRequest request) {
        repository.save(new Pedido(request.cliente()));
    }
}
