package com.exemplo.loja.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class Pedido {

    @Id
    @GeneratedValue
    private Long id;
    private String cliente;

    protected Pedido() {
    }

    public Pedido(String cliente) {
        this.cliente = cliente;
    }

    public Long getId() {
        return id;
    }

    public String getCliente() {
        return cliente;
    }
}
