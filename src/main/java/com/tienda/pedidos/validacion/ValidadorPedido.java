package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ResultadoPedido;

/**
 * Clase base abstracta para la Cadena de Responsabilidad (Chain of Responsibility).
 * Implementa el metodo plantilla 'validar' con corte anticipado (fail-fast).
 */
public abstract class ValidadorPedido {

    private ValidadorPedido siguiente;

    /**
     * Conecta el siguiente eslabon en la cadena y retorna dicho eslabon.
     *
     * @param siguiente Eslabon posterior
     * @return El eslabon recibido
     */
    public ValidadorPedido encadenar(ValidadorPedido siguiente) {
        this.siguiente = siguiente;
        return siguiente;
    }

    /**
     * Metodo plantilla que ejecuta la validacion propia y, si pasa satisfactoriamente,
     * delega al siguiente eslabon de la cadena.
     *
     * @param contexto Informacion compartida del pedido
     * @return ResultadoPedido con rechazo si falla algun eslabon, o null si la cadena completa pasa
     */
    public ResultadoPedido validar(ContextoPedido contexto) {
        ResultadoPedido resultado = ejecutarValidacion(contexto);
        if (resultado != null && !resultado.isConfirmado()) {
            return resultado; // Corte anticipado: si un eslabon rechaza, se detiene la evaluacion
        }
        if (siguiente != null) {
            return siguiente.validar(contexto);
        }
        return null; // Cadena superada sin errores
    }

    /**
     * Logica de validacion especifica de cada eslabon.
     *
     * @param contexto Datos del pedido y estado acumulado
     * @return ResultadoPedido.rechazado si no cumple las reglas, o null si la validacion es exitosa
     */
    protected abstract ResultadoPedido ejecutarValidacion(ContextoPedido contexto);
}
