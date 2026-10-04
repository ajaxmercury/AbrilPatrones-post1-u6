package com.tienda.pedidos.descuento;

import com.tienda.pedidos.service.ControlBlackFriday;
import com.tienda.pedidos.validacion.ContextoPedido;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Estrategia de descuento para campana Black Friday.
 * Retorna 25% si la campana esta activa, o 0% en caso contrario.
 */
@Component
public class DescuentoBlackFriday implements EstrategiaDescuento, ControlBlackFriday {

    private boolean activa;

    public DescuentoBlackFriday(@Value("${promo.black-friday.activa:false}") boolean activa) {
        this.activa = activa;
    }

    @Override
    public boolean isActiva() {
        return activa;
    }

    @Override
    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public double calcularPorcentaje(ContextoPedido contexto, double subtotal) {
        return this.activa ? 0.25 : 0.0;
    }
}
