package com.exemplo.loja.service;

import com.exemplo.loja.repository.PedidoRepository;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    public long contar() {
        return pedidoRepository.count();
    }
}
