package com.exemplo.loja.pedido;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Entity;

@Entity
public class Pedido extends PanacheEntity {

    public String cliente;

    public long totalEmCentavos;
}
