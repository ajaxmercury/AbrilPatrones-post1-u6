package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Componente que encapsula la resolucion definitiva del descuento.
 * Aplica el principio de diseño correcto:
 * 1. Calcula el descuento aplicable segun el tipo de cliente.
 * 2. Evalua las campanas promocionales vigentes (Black Friday, Corporativo, Volumen).
 * 3. Selecciona el mayor porcentaje entre el beneficio por tipo de cliente y el mayor de las campanas.
 */
@Component
public class CalculadorDescuentoFinal {

    private static final Logger log = LoggerFactory.getLogger(CalculadorDescuentoFinal.class);

    private final SelectorEstrategiaDescuento selectorTipoCliente;
    private final List<EstrategiaDescuento> estrategiasCampanas;

    public CalculadorDescuentoFinal(SelectorEstrategiaDescuento selectorTipoCliente,
                                    DescuentoBlackFriday blackFriday,
                                    DescuentoCorporativo corporativo,
                                    DescuentoVolumen volumen) {
        this.selectorTipoCliente = selectorTipoCliente;
        this.estrategiasCampanas = List.of(blackFriday, corporativo, volumen);
    }

    /**
     * Resuelve el monto final de descuento comercial a deducir del subtotal.
     *
     * @param contexto Datos del pedido y del cliente
     * @param subtotal Subtotal bruto de la orden
     * @return Valor monetario a descontar
     */
    public double calcularDescuentoFinal(ContextoPedido contexto, double subtotal) {
        // 1. Descuento determinado por el tipo de cliente
        EstrategiaDescuento estrategiaCliente = selectorTipoCliente.seleccionar(contexto.getTipoCliente());
        double porcentajeCliente = estrategiaCliente.calcularPorcentaje(contexto, subtotal);

        // 2. Mayor beneficio entre las campanas promocionales
        double mayorCampana = 0.0;
        for (EstrategiaDescuento campana : estrategiasCampanas) {
            double pct = campana.calcularPorcentaje(contexto, subtotal);
            if (pct > mayorCampana) {
                mayorCampana = pct;
            }
        }

        // 3. El mayor porcentaje entre el descuento por tipo de cliente y el mayor de las campanas
        double porcentajeGanador = Math.max(porcentajeCliente, mayorCampana);
        log.debug("Evaluacion de descuentos: TipoCliente={}%, MayorCampana={}% -> Ganador={}%",
                (porcentajeCliente * 100), (mayorCampana * 100), (porcentajeGanador * 100));

        return subtotal * porcentajeGanador;
    }
}
