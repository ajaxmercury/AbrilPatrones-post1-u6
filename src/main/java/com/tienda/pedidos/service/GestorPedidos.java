package com.tienda.pedidos.service;

import com.tienda.pedidos.descuento.EstrategiaDescuento;
import com.tienda.pedidos.descuento.SelectorEstrategiaDescuento;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.PromocionBlackFriday;
import com.tienda.pedidos.validacion.PromocionCorporativo;
import com.tienda.pedidos.validacion.PromocionVolumen;
import com.tienda.pedidos.validacion.ValidadorCliente;
import com.tienda.pedidos.validacion.ValidadorPedido;
import com.tienda.pedidos.validacion.ValidadorStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * GestorPedidos con implementacion del antipatron Golden Hammer:
 * Se reutiliza la Cadena de Responsabilidad para inyectar eslabones de promociones comerciales
 * que mutan el contexto y compiten por Math.max, violando el contrato de validacion.
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
                         PromocionBlackFriday promoBlackFriday,
                         PromocionCorporativo promoCorporativo,
                         PromocionVolumen promoVolumen,
                         CalculadorSubtotal calculadorSubtotal,
                         SelectorEstrategiaDescuento selectorDescuento,
                         PedidoRepository pedidoRepository,
                         NotificacionPedidoService notificacionService) {
        // Cableado de los 5 eslabones con correccion obligatoria preservando la cabeza
        validadorStock.encadenar(validadorCliente)
                      .encadenar(promoBlackFriday)
                      .encadenar(promoCorporativo)
                      .encadenar(promoVolumen);
        this.primerValidador = validadorStock;

        this.calculadorSubtotal = calculadorSubtotal;
        this.selectorDescuento = selectorDescuento;
        this.pedidoRepository = pedidoRepository;
        this.notificacionService = notificacionService;
    }

    /**
     * Procesa una solicitud de pedido delegando en la cadena con 5 eslabones y resolviendo
     * el mayor descuento entre el tipo de cliente y el estado mutado por las campanas.
     *
     * @param request Solicitud con cliente y lista de items
     * @return ResultadoPedido confirmado con ID y total, o rechazado con motivo especifico
     */
    public ResultadoPedido procesarPedido(PedidoRequest request) {
        log.info("Orquestando procesamiento de pedido para cliente ID: {}", request.getClienteId());

        // 1. CAPA DE VALIDACION (Y campanas promocionales indebidamente acopladas en la cadena)
        ContextoPedido contexto = new ContextoPedido(request);
        ResultadoPedido falloValidacion = primerValidador.validar(contexto);
        if (falloValidacion != null) {
            log.warn("Pedido rechazado en la cadena de validacion: {}", falloValidacion.getMotivo());
            return falloValidacion;
        }

        // 2. CAPA DE CALCULO FINANCIERO Y DESCUENTOS (Competencia por el maximo)
        CalculadorSubtotal.SubtotalDetalle detalleSubtotal = calculadorSubtotal.calcularSubtotal(request.getItems());
        double subtotal = detalleSubtotal.subtotal();

        EstrategiaDescuento estrategia = selectorDescuento.seleccionar(contexto.getTipoCliente());
        double porcentajeTipoCliente = estrategia.calcularPorcentaje(contexto, subtotal);
        double porcentajeFinal = Math.max(porcentajeTipoCliente, contexto.getDescuentoCampana());
        double descuento = subtotal * porcentajeFinal;

        double baseImponible = subtotal - descuento;
        double impuesto = baseImponible * 0.19; // IVA 19% identico a la formula original
        double total = baseImponible + impuesto;

        log.debug("Subtotal: {}, Descuento final: {} ({}%), Base: {}, IVA: {}, Total: {}",
                subtotal, descuento, (porcentajeFinal * 100), baseImponible, impuesto, total);

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
