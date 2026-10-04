package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.CalculadorDescuentoFinal;
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
 * GestorPedidos refactorizado definitivamente (Arquitectura Limpia).
 * Orquestador delgado de cuatro capas:
 * 1. Validacion (Chain of Responsibility con corte anticipado: solo ValidadorStock y ValidadorCliente).
 * 2. Calculo y Descuento (Strategy Pattern coordinado por CalculadorDescuentoFinal).
 * 3. Persistencia (PedidoRepository con GeneratedKeyHolder).
 * 4. Notificacion (NotificacionPedidoService).
 */
@Service
public class GestorPedidos {

    private static final Logger log = LoggerFactory.getLogger(GestorPedidos.class);

    private final ValidadorPedido primerValidador;
    private final CalculadorSubtotal calculadorSubtotal;
    private final CalculadorDescuentoFinal calculadorDescuentoFinal;
    private final PedidoRepository pedidoRepository;
    private final NotificacionPedidoService notificacionService;

    public GestorPedidos(ValidadorStock validadorStock,
                         ValidadorCliente validadorCliente,
                         CalculadorSubtotal calculadorSubtotal,
                         CalculadorDescuentoFinal calculadorDescuentoFinal,
                         PedidoRepository pedidoRepository,
                         NotificacionPedidoService notificacionService) {
        // La Cadena de Responsabilidad conserva unica y exclusivamente validaciones de integridad
        validadorStock.encadenar(validadorCliente);
        this.primerValidador = validadorStock;

        this.calculadorSubtotal = calculadorSubtotal;
        this.calculadorDescuentoFinal = calculadorDescuentoFinal;
        this.pedidoRepository = pedidoRepository;
        this.notificacionService = notificacionService;
    }

    /**
     * Orquesta el procesamiento limpio del pedido sin acoplamiento espagueti ni mutacion en la cadena.
     *
     * @param request Solicitud con cliente y lista de items
     * @return ResultadoPedido confirmado con ID y total, o rechazado con motivo explicito
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

        double descuento = calculadorDescuentoFinal.calcularDescuentoFinal(contexto, subtotal);
        double baseImponible = subtotal - descuento;
        double impuesto = baseImponible * 0.19; // IVA 19% identico al original
        double total = baseImponible + impuesto;

        log.debug("Subtotal: {}, Descuento: {}, Base: {}, IVA: {}, Total: {}",
                subtotal, descuento, baseImponible, impuesto, total);

        // 3. CAPA DE PERSISTENCIA (Repository Pattern)
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
