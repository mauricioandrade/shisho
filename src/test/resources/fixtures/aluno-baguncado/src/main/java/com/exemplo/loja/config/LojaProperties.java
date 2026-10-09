package com.exemplo.loja.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "loja")
public class LojaProperties {

    private int limiteUsuarios;

    public int getLimiteUsuarios() {
        return limiteUsuarios;
    }

    public void setLimiteUsuarios(int limiteUsuarios) {
        this.limiteUsuarios = limiteUsuarios;
    }
}
