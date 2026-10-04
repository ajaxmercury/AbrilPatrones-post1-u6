package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

/**
 * Estrategia de descuento para clientes frecuentes.
 * Premia la fidelidad segun el volumen de pedidos previos registrados:
 * - Mas de 10 pedidos: 8%
 * - Mas de 3 pedidos: 4%
 * - Hasta 3 pedidos: 0%
 */
@Component
public class DescuentoFrecuente implements EstrategiaDescuento {

    @Override
    public double calcularPorcentaje(ContextoPedido contexto, double subtotal) {
        int pedidosPrevios = contexto.getPedidosPrevios();
        if (pedidosPrevios > 10) {
            return 0.08;
        } else if (pedidosPrevios > 3) {
            return 0.04;
        } else {
            return 0.0;
        }
    }
}
