package com.tienda.pedidos.service;

/**
 * Contrato de configuracion dinamica para la campana de Black Friday.
 */
public interface ControlBlackFriday {
    boolean isActiva();
    void setActiva(boolean activa);
}
