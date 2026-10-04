package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.PedidoRequest;
import org.springframework.stereotype.Service;

/**
 * Servicio dedicado al formateo y transmision de notificaciones sobre el estado de pedidos.
 * Aislado para cumplir con el Principio de Responsabilidad Unica (SRP).
 */
@Service
public class NotificacionPedidoService {

    private final EmailService emailService;

    public NotificacionPedidoService(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Construye el mensaje con los datos del pedido confirmado y delega el envio a EmailService.
     *
     * @param pedidoId Identificador del pedido confirmado
     * @param request Solicitud original con datos del cliente
     * @param total Monto final pagado
     * @param descuento Monto deducido por beneficios
     */
    public void notificarConfirmacion(Long pedidoId, PedidoRequest request, double total, double descuento) {
        String cuerpo = construirCuerpoCorreo(pedidoId, request, total, descuento);
        emailService.enviar(request.getClienteEmail(), "Confirmación de Pedido #" + pedidoId, cuerpo);
    }

    private String construirCuerpoCorreo(Long pedidoId, PedidoRequest request, double total, double descuento) {
        StringBuilder cuerpo = new StringBuilder();
        cuerpo.append("Estimado cliente,\n\n");
        cuerpo.append("Su pedido #").append(pedidoId).append(" ha sido confirmado con éxito.\n");
        cuerpo.append("Resumen de compra:\n");
        cuerpo.append("- Cliente ID: ").append(request.getClienteId()).append("\n");
        cuerpo.append("- Cantidad de productos: ").append(request.getItems().size()).append("\n");
        cuerpo.append("- Descuento aplicado: $").append(String.format("%.2f", descuento)).append("\n");
        cuerpo.append("- Total a pagar (IVA incluido): $").append(String.format("%.2f", total)).append("\n\n");
        cuerpo.append("Agradecemos su preferencia.\n");
        cuerpo.append("Atentamente,\nEquipo de Atencion al Cliente.\n");
        return cuerpo.toString();
    }
}
