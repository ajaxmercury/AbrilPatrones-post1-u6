package com.tienda.pedidos.descuento;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Selector que resuelve la EstrategiaDescuento adecuada segun el tipo de cliente.
 * Mantiene un mapa asociativo y aplica por defecto la estrategia estandar.
 */
@Component
public class SelectorEstrategiaDescuento {

    private final Map<String, EstrategiaDescuento> estrategias;
    private final EstrategiaDescuento estrategiaPorDefecto;

    public SelectorEstrategiaDescuento(DescuentoVip vip,
                                       DescuentoFrecuente frecuente,
                                       DescuentoEstandar estandar) {
        this.estrategias = new HashMap<>();
        this.estrategias.put("VIP", vip);
        this.estrategias.put("FRECUENTE", frecuente);
        this.estrategias.put("ESTANDAR", estandar);
        this.estrategiaPorDefecto = estandar;
    }

    /**
     * Retorna la estrategia correspondiente al tipo de cliente provisto.
     *
     * @param tipoCliente Identificador del tipo de cliente (VIP, FRECUENTE, ESTANDAR)
     * @return Estrategia de descuento aplicable (o estrategia por defecto si es nulo/desconocido)
     */
    public EstrategiaDescuento seleccionar(String tipoCliente) {
        if (tipoCliente == null) {
            return estrategiaPorDefecto;
        }
        return estrategias.getOrDefault(tipoCliente.toUpperCase(), estrategiaPorDefecto);
    }
}
