package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ResultadoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Eslabon de promocion corporativa incorporado a la cadena de validacion (Golden Hammer).
 * Aplica un 10% si el cliente posee NIT, compitiendo mediante Math.max sobre un estado mutable.
 */
@Component
public class PromocionCorporativo extends ValidadorPedido {

    private static final Logger log = LoggerFactory.getLogger(PromocionCorporativo.class);

    @Override
    protected ResultadoPedido ejecutarValidacion(ContextoPedido contexto) {
        String nit = contexto.getNit();
        if (nit != null && !nit.trim().isEmpty()) {
            log.info("Cliente con NIT detectado ({}): aplicando 10% de descuento corporativo", nit);
            contexto.aplicarDescuentoCampana(0.10);
        }
        // Viola el contrato de ValidadorPedido: nunca rechaza
        return null;
    }
}
