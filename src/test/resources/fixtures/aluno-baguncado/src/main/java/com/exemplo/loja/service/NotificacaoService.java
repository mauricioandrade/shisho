package com.exemplo.loja.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class NotificacaoService {

    private static final ThreadLocal<String> USUARIO_ATUAL = new ThreadLocal<>();

    private final ExecutorService executor = Executors.newFixedThreadPool(20);

    public void notificar(String usuario, Runnable envio) {
        USUARIO_ATUAL.set(usuario);
        executor.submit(envio);
    }
}
