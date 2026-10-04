package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

/**
 * Estrategia de descuento para clientes estandar.
 * Aplica la tarifa base sin descuentos adicionales (0%).
 */
@Component
public class DescuentoEstandar implements EstrategiaDescuento {

    @Override
    public double calcularPorcentaje(ContextoPedido contexto, double subtotal) {
        return 0.0;
    }
}
