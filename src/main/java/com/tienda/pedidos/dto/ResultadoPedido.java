package com.tienda.pedidos.dto;

public class ResultadoPedido {
    private final boolean confirmado;
    private final Long pedidoId;
    private final String motivo;
    private final double total;

    private ResultadoPedido(boolean confirmado, Long pedidoId, String motivo, double total) {
        this.confirmado = confirmado;
        this.pedidoId = pedidoId;
        this.motivo = motivo;
        this.total = total;
    }

    public static ResultadoPedido confirmado(Long pedidoId, double total) {
        return new ResultadoPedido(true, pedidoId, null, total);
    }

    public static ResultadoPedido confirmado(double total) {
        return new ResultadoPedido(true, null, null, total);
    }

    public static ResultadoPedido rechazado(String motivo) {
        return new ResultadoPedido(false, null, motivo, 0.0);
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public String getMotivo() {
        return motivo;
    }

    public double getTotal() {
        return total;
    }

    @Override
    public String toString() {
        return "ResultadoPedido{" +
                "confirmado=" + confirmado +
                ", pedidoId=" + pedidoId +
                ", motivo='" + motivo + '\'' +
                ", total=" + total +
                '}';
    }
}
