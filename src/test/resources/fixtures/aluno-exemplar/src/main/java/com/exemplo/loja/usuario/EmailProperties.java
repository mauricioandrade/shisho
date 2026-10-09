package com.exemplo.loja.usuario;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("loja.email")
record EmailProperties(String host, int porta, String remetente) {
}
