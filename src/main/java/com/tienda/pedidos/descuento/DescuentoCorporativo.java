package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

/**
 * Estrategia de descuento para clientes corporativos.
 * Retorna 10% si el cliente posee un numero de NIT registrado.
 */
@Component
public class DescuentoCorporativo implements EstrategiaDescuento {

    @Override
    public double calcularPorcentaje(ContextoPedido contexto, double subtotal) {
        String nit = contexto.getNit();
        if (nit != null && !nit.trim().isEmpty()) {
            return 0.10;
        }
        return 0.0;
    }
}
