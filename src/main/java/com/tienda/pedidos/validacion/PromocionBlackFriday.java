package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.ControlBlackFriday;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Eslabon de promocion Black Friday incorporado indebidamente a la cadena de validacion (Golden Hammer).
 * Aplica un 25% de descuento si la campana esta activa, mutando el contexto sin validar ni rechazar.
 */
@Component
public class PromocionBlackFriday extends ValidadorPedido implements ControlBlackFriday {

    private static final Logger log = LoggerFactory.getLogger(PromocionBlackFriday.class);

    private boolean activa;

    public PromocionBlackFriday(@Value("${promo.black-friday.activa:false}") boolean activa) {
        this.activa = activa;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    protected ResultadoPedido ejecutarValidacion(ContextoPedido contexto) {
        if (this.activa) {
            log.info("Campana Black Friday activa: aplicando 25% de descuento en el contexto");
            contexto.aplicarDescuentoCampana(0.25);
        }
        // Viola el contrato de ValidadorPedido: nunca rechaza
        return null;
    }
}
