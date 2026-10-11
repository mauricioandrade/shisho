package com.exemplo.loja.pedido;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
class FreteService {

    @ConfigProperty(name = "loja.frete.base")
    long base;

    @ConfigProperty(name = "loja.frete.por-quilo")
    long porQuilo;

    @ConfigProperty(name = "loja.frete.gratis-acima-de")
    long gratisAcimaDe;

    long calcular(long totalEmCentavos, int quilos) {
        return totalEmCentavos > gratisAcimaDe ? 0 : base + porQuilo * quilos;
    }
}
