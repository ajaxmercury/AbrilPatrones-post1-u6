package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.EstrategiaDescuento;
import com.tienda.pedidos.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorPedido;
import com.tienda.pedidos.validacion.ValidadorStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * GestorPedidos refactorizado — Orquestador delgado de las cuatro capas de la aplicacion:
 * 1. Capa de Validacion (Chain of Responsibility con corte anticipado).
 * 2. Capa de Calculo y Beneficios Comerciales (Strategy Pattern para descuentos).
 * 3. Capa de Persistencia (Repository Pattern para aislamiento relacional).
 * 4. Capa de Notificacion (Servicio de mensajeria y correo electronico).
 */
@Service
public class GestorPedidos {

    private static final Logger log = LoggerFactory.getLogger(GestorPedidos.class);

    private final ValidadorPedido primerValidador;
    private final CalculadorSubtotal calculadorSubtotal;
    private final SelectorEstrategiaDescuento selectorDescuento;
    private final PedidoRepository pedidoRepository;
    private final NotificacionPedidoService notificacionService;

    public GestorPedidos(ValidadorStock validadorStock,
                         ValidadorCliente validadorCliente,
                         CalculadorSubtotal calculadorSubtotal,
                         SelectorEstrategiaDescuento selectorDescuento,
                         PedidoRepository pedidoRepository,
                         NotificacionPedidoService notificacionService) {
        // Correccion obligatoria del bug de cableado de la guia:
        // encadenar() devuelve el siguiente eslabon, por lo que se debe preservar la referencia a la cabeza.
        validadorStock.encadenar(validadorCliente);
        this.primerValidador = validadorStock;

        this.calculadorSubtotal = calculadorSubtotal;
        this.selectorDescuento = selectorDescuento;
        this.pedidoRepository = pedidoRepository;
        this.notificacionService = notificacionService;
    }

    /**
     * Procesa una solicitud de pedido delegando ordenadamente en los componentes especializados.
     *
     * @param request Solicitud con cliente y lista de items
     * @return ResultadoPedido confirmado con ID y total, o rechazado con motivo especifico
     */
    public ResultadoPedido procesarPedido(PedidoRequest request) {
        log.info("Orquestando procesamiento de pedido para cliente ID: {}", request.getClienteId());

        // 1. CAPA DE VALIDACION (Chain of Responsibility)
        ContextoPedido contexto = new ContextoPedido(request);
        ResultadoPedido falloValidacion = primerValidador.validar(contexto);
        if (falloValidacion != null) {
            log.warn("Pedido rechazado en la capa de validacion: {}", falloValidacion.getMotivo());
            return falloValidacion;
        }

        // 2. CAPA DE CALCULO FINANCIERO Y DESCUENTOS (Strategy Pattern)
        CalculadorSubtotal.SubtotalDetalle detalleSubtotal = calculadorSubtotal.calcularSubtotal(request.getItems());
        double subtotal = detalleSubtotal.subtotal();

        EstrategiaDescuento estrategia = selectorDescuento.seleccionar(contexto.getTipoCliente());
        double porcentaje = estrategia.calcularPorcentaje(contexto, subtotal);
        double descuento = subtotal * porcentaje;

        double baseImponible = subtotal - descuento;
        double impuesto = baseImponible * 0.19; // IVA 19% identico a la formula original
        double total = baseImponible + impuesto;

        log.debug("Subtotal: {}, Descuento: {} ({}%), Base: {}, IVA: {}, Total: {}",
                subtotal, descuento, (porcentaje * 100), baseImponible, impuesto, total);

        // 3. CAPA DE PERSISTENCIA (Repository Pattern con GeneratedKeyHolder)
        Long pedidoId = pedidoRepository.guardarPedido(
                request.getClienteId(),
                total,
                request.getItems(),
                detalleSubtotal.preciosUnitarios()
        );

        // 4. CAPA DE NOTIFICACION
        notificacionService.notificarConfirmacion(pedidoId, request, total, descuento);

        log.info("Pedido #{} procesado y confirmado exitosamente", pedidoId);
        return ResultadoPedido.confirmado(pedidoId, total);
    }
}
