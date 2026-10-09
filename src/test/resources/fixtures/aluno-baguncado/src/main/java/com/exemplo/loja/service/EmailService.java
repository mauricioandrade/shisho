package com.exemplo.loja.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final String host;
    private final int porta;
    private final String remetente;

    public EmailService(
            @Value("${loja.email.host}") String host,
            @Value("${loja.email.porta}") int porta,
            @Value("${loja.email.remetente}") String remetente) {
        this.host = host;
        this.porta = porta;
        this.remetente = remetente;
    }
}
