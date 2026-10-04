package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.ResultadoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Eslabon de promocion por volumen incorporado a la cadena de validacion (Golden Hammer).
 * Aplica un 12% si la orden supera 20 unidades en total.
 */
@Component
public class PromocionVolumen extends ValidadorPedido {

    private static final Logger log = LoggerFactory.getLogger(PromocionVolumen.class);

    @Override
    protected ResultadoPedido ejecutarValidacion(ContextoPedido contexto) {
        int totalUnidades = 0;
        if (contexto.getRequest().getItems() != null) {
            for (ItemPedido item : contexto.getRequest().getItems()) {
                totalUnidades += item.getCantidad();
            }
        }

        if (totalUnidades > 20) {
            log.info("Volumen superior a 20 unidades (total: {}): aplicando 12% de descuento", totalUnidades);
            contexto.aplicarDescuentoCampana(0.12);
        }
        // Viola el contrato de ValidadorPedido: nunca rechaza
        return null;
    }
}
