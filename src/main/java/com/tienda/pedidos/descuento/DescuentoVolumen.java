package com.tienda.pedidos.descuento;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.stereotype.Component;

/**
 * Estrategia de descuento comercial por volumen de compra.
 * Retorna 12% si la orden suma mas de 20 unidades en total.
 */
@Component
public class DescuentoVolumen implements EstrategiaDescuento {

    @Override
    public double calcularPorcentaje(ContextoPedido contexto, double subtotal) {
        int totalUnidades = 0;
        if (contexto.getRequest().getItems() != null) {
            for (ItemPedido item : contexto.getRequest().getItems()) {
                totalUnidades += item.getCantidad();
            }
        }
        return totalUnidades > 20 ? 0.12 : 0.0;
    }
}
