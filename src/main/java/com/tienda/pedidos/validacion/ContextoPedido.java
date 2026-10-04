package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.PedidoRequest;

/**
 * Contexto que encapsula los datos de la solicitud y el estado acumulado
 * durante el ciclo de validacion y procesamiento del pedido.
 */
public class ContextoPedido {

    private final PedidoRequest request;
    private String tipoCliente;
    private String nit;
    private int pedidosPrevios;

    public ContextoPedido(PedidoRequest request) {
        this.request = request;
    }

    public PedidoRequest getRequest() {
        return request;
    }

    public String getTipoCliente() {
        return tipoCliente;
    }

    public void setTipoCliente(String tipoCliente) {
        this.tipoCliente = tipoCliente;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public int getPedidosPrevios() {
        return pedidosPrevios;
    }

    public void setPedidosPrevios(int pedidosPrevios) {
        this.pedidosPrevios = pedidosPrevios;
    }

    private double descuentoCampana = 0.0;

    public void aplicarDescuentoCampana(double descuento) {
        this.descuentoCampana = Math.max(this.descuentoCampana, descuento);
    }

    public double getDescuentoCampana() {
        return descuentoCampana;
    }
}
