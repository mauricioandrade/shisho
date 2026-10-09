package com.exemplo.loja.service;

import com.exemplo.loja.controller.Pagina;
import com.exemplo.loja.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PedidoService {

    private PedidoRepository pedidoRepository;

    public PedidoService(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional
    public long contar() {
        return pedidoRepository.count();
    }

    public Pagina<Long> contarPorPagina() {
        return new Pagina<>(List.of(contar()), 1);
    }
}
