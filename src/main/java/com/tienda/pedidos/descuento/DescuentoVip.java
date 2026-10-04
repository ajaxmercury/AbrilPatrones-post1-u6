package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

/**
 * Estrategia de descuento para clientes VIP.
 * Escala segun el monto bruto de compra:
 * - Mayor a $1,000,000: 15%
 * - Mayor a $500,000: 10%
 * - En caso base: 5%
 */
@Component
public class DescuentoVip implements EstrategiaDescuento {

    @Override
    public double calcularPorcentaje(ContextoPedido contexto, double subtotal) {
        if (subtotal > 1000000.0) {
            return 0.15;
        } else if (subtotal > 500000.0) {
            return 0.10;
        } else {
            return 0.05;
        }
    }
}
