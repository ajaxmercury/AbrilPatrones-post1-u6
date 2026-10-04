package com.tienda.pedidos.descuento;

import com.tienda.pedidos.validacion.ContextoPedido;

/**
 * Interfaz de la estrategia de descuento (Patron Strategy).
 * Permite encapsular las diferentes politicas de beneficios comerciales
 * de manera intercambiable sin modificar el orquestador principal.
 */
public interface EstrategiaDescuento {

    /**
     * Calcula la fraccion porcentual de descuento aplicable (entre 0.0 y 1.0).
     *
     * @param contexto Datos del pedido y del cliente
     * @param subtotal Valor bruto acumulado de los productos
     * @return Porcentaje de descuento en formato decimal
     */
    double calcularPorcentaje(ContextoPedido contexto, double subtotal);
}
