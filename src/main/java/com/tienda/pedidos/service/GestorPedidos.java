package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GestorPedidos - Implementacion monolítica original (Línea base).
 * Este componente concentra múltiples responsabilidades violando el principio de responsabilidad única (SRP),
 * acoplando persistencia, reglas de negocio, validaciones, lógica fiscal y formateo de notificaciones.
 *
 * Antipatrones identificados:
 * - God Object: Acumula el flujo completo de negocio y metodos auxiliares no cohesivos.
 * - Spaghetti Code: Logica anidada de descuentos y reglas de mora sin separacion de capas.
 */
@Service
public class GestorPedidos {

    private static final Logger log = LoggerFactory.getLogger(GestorPedidos.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EmailService emailService;

    /**
     * Procesa un pedido en una sola rutina monolítica.
     * Realiza validación de inventario, comprobación de estado crediticio, cálculo de subtotal,
     * cálculo de descuentos condicionales anidados, liquidación tributaria, persistencia SQL
     * y envío de correo electrónico.
     *
     * @param request Datos del pedido solicitado
     * @return ResultadoPedido con estado confirmado o rechazado
     */
    public ResultadoPedido procesarPedido(PedidoRequest request) {
        log.info("Iniciando procesamiento de pedido para cliente ID: {}", request.getClienteId());

        // ---------------------------------------------------------
        // 1. VALIDACION DE STOCK EN INVENTARIO
        // ---------------------------------------------------------
        if (request.getItems() == null || request.getItems().isEmpty()) {
            log.warn("El pedido no contiene items");
            return ResultadoPedido.rechazado("El pedido no contiene productos");
        }

        for (ItemPedido item : request.getItems()) {
            String sqlStock = "SELECT cantidad FROM inventario WHERE producto_id = ?";
            Integer stock = jdbcTemplate.queryForObject(sqlStock, Integer.class, item.getProductoId());
            if (stock == null || stock < item.getCantidad()) {
                log.warn("Stock insuficiente para el producto ID: {}. Solicitado: {}, Disponible: {}",
                        item.getProductoId(), item.getCantidad(), stock);
                return ResultadoPedido.rechazado("Stock insuficiente: producto " + item.getProductoId());
            }
        }

        // ---------------------------------------------------------
        // 2. VALIDACION DE CLIENTE Y POLITICA DE CORTE DE MORA
        // ---------------------------------------------------------
        String sqlCliente = "SELECT tipo_cliente FROM clientes WHERE id = ?";
        String tipoCliente = jdbcTemplate.queryForObject(sqlCliente, String.class, request.getClienteId());

        String sqlDeuda = "SELECT COUNT(*) FROM facturas WHERE cliente_id = ? AND pagada = false";
        Integer facturasPendientes = jdbcTemplate.queryForObject(sqlDeuda, Integer.class, request.getClienteId());

        if (facturasPendientes != null && facturasPendientes > 0) {
            LocalTime ahora = LocalTime.now();
            log.debug("Hora actual de evaluacion de mora: {}", ahora);
            if (ahora.isBefore(LocalTime.of(20, 0))) {
                log.warn("Cliente {} rechazado por facturas pendientes antes del horario de corte", request.getClienteId());
                return ResultadoPedido.rechazado("Cliente con facturas pendientes");
            } else {
                log.info("Cliente {} presenta mora pero supera el horario de corte de las 20:00", request.getClienteId());
            }
        }

        // ---------------------------------------------------------
        // 3. CALCULO DE SUBTOTAL
        // ---------------------------------------------------------
        double subtotal = 0.0;
        for (ItemPedido item : request.getItems()) {
            String sqlPrecio = "SELECT precio FROM productos WHERE id = ?";
            Double precio = jdbcTemplate.queryForObject(sqlPrecio, Double.class, item.getProductoId());
            if (precio != null) {
                subtotal += precio * item.getCantidad();
            }
        }
        log.debug("Subtotal calculado para el pedido: {}", subtotal);

        // ---------------------------------------------------------
        // 4. CALCULO DE DESCUENTO SEGUN TIPO DE CLIENTE (SPAGHETTI)
        // ---------------------------------------------------------
        double porcentajeDescuento = 0.0;
        if ("VIP".equalsIgnoreCase(tipoCliente)) {
            if (subtotal > 1000000.0) {
                porcentajeDescuento = 0.15;
            } else if (subtotal > 500000.0) {
                porcentajeDescuento = 0.10;
            } else {
                porcentajeDescuento = 0.05;
            }
        } else if ("FRECUENTE".equalsIgnoreCase(tipoCliente)) {
            String sqlConteoPedidos = "SELECT COUNT(*) FROM pedidos WHERE cliente_id = ?";
            Integer pedidosPrevios = jdbcTemplate.queryForObject(sqlConteoPedidos, Integer.class, request.getClienteId());
            if (pedidosPrevios != null && pedidosPrevios > 10) {
                porcentajeDescuento = 0.08;
            } else if (pedidosPrevios != null && pedidosPrevios > 3) {
                porcentajeDescuento = 0.04;
            } else {
                porcentajeDescuento = 0.0;
            }
        } else {
            porcentajeDescuento = 0.0;
        }

        double descuento = subtotal * porcentajeDescuento;
        log.debug("Descuento aplicado: {} ({}%)", descuento, (porcentajeDescuento * 100));

        // ---------------------------------------------------------
        // 5. CALCULO DE IMPUESTO Y TOTAL FINAL
        // ---------------------------------------------------------
        double baseImponible = subtotal - descuento;
        double impuesto = baseImponible * 0.19;
        double total = baseImponible + impuesto;
        log.debug("Base: {}, Impuesto (19%): {}, Total: {}", baseImponible, impuesto, total);

        // ---------------------------------------------------------
        // 6. PERSISTENCIA DE PEDIDO, DETALLE Y ACTUALIZACION STOCK
        // ---------------------------------------------------------
        String sqlInsertPedido = "INSERT INTO pedidos (cliente_id, total, fecha) VALUES (?, ?, CURRENT_TIMESTAMP)";
        jdbcTemplate.update(sqlInsertPedido, request.getClienteId(), total);

        String sqlIdentity = "CALL IDENTITY()";
        Long pedidoId = jdbcTemplate.queryForObject(sqlIdentity, Long.class);
        log.info("Pedido insertado exitosamente con ID generado: {}", pedidoId);

        for (ItemPedido item : request.getItems()) {
            String sqlPrecio = "SELECT precio FROM productos WHERE id = ?";
            Double precioUnitario = jdbcTemplate.queryForObject(sqlPrecio, Double.class, item.getProductoId());

            String sqlInsertDetalle = "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(sqlInsertDetalle, pedidoId, item.getProductoId(), item.getCantidad(), precioUnitario);

            String sqlActualizarStock = "UPDATE inventario SET cantidad = cantidad - ? WHERE producto_id = ?";
            jdbcTemplate.update(sqlActualizarStock, item.getCantidad(), item.getProductoId());
        }

        // ---------------------------------------------------------
        // 7. CONSTRUCCION Y ENVIO DE NOTIFICACION POR CORREO
        // ---------------------------------------------------------
        String cuerpoCorreo = construirCuerpoCorreo(pedidoId, request, total, descuento);
        emailService.enviar(request.getClienteEmail(), "Confirmación de Pedido #" + pedidoId, cuerpoCorreo);

        log.info("Procesamiento finalizado con éxito para pedido ID: {}", pedidoId);
        return ResultadoPedido.confirmado(pedidoId, total);
    }

    // =========================================================================
    // METODOS AUXILIARES PRIVADOS (ACUMULADOS EN EL GOD OBJECT)
    // =========================================================================

    /**
     * Recupera el historial reciente de pedidos para auditoria o análisis de consumo.
     *
     * @param clienteId Identificador del cliente
     * @param limite Numero maximo de registros
     * @return Lista estructurada de registros históricos
     */
    private List<Map<String, Object>> obtenerHistorialCliente(Long clienteId, int limite) {
        log.debug("Consultando historial comercial para cliente: {}, limite: {}", clienteId, limite);
        String sql = "SELECT id, total, fecha FROM pedidos WHERE cliente_id = ? ORDER BY fecha DESC";
        List<Map<String, Object>> resultados = new ArrayList<>();
        try {
            List<Map<String, Object>> filas = jdbcTemplate.queryForList(sql, clienteId);
            int contador = 0;
            for (Map<String, Object> fila : filas) {
                if (contador >= limite) {
                    break;
                }
                Map<String, Object> elemento = new HashMap<>();
                elemento.put("id", fila.get("id"));
                elemento.put("total", fila.get("total"));
                elemento.put("fecha", fila.get("fecha"));
                resultados.add(elemento);
                contador++;
            }
        } catch (Exception e) {
            log.error("Error al obtener historial del cliente {}: {}", clienteId, e.getMessage());
        }
        return resultados;
    }

    /**
     * Genera una representación textual formateada de la factura comercial.
     *
     * @param pedidoId ID del pedido
     * @param subtotal Monto bruto
     * @param impuesto Monto impositivo
     * @param total Monto neto liquidado
     * @return Documento de texto formateado
     */
    private String formatearFactura(Long pedidoId, double subtotal, double impuesto, double total) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("            FACTURA DE VENTA             \n");
        sb.append("=========================================\n");
        sb.append(String.format("Numero de Orden:    #%08d%n", pedidoId));
        sb.append(String.format("Subtotal Bruto:     $%12.2f%n", subtotal));
        sb.append(String.format("IVA Liquidado (19%): $%12.2f%n", impuesto));
        sb.append("-----------------------------------------\n");
        sb.append(String.format("TOTAL A PAGAR:      $%12.2f%n", total));
        sb.append("=========================================\n");
        sb.append("      Gracias por confiar en nosotros    \n");
        return sb.toString();
    }

    /**
     * Aplica reglas tributarias territoriales diferenciadas según la región del envío.
     *
     * @param region Codigo o nombre del departamento o zona
     * @param subtotal Monto base para la aplicación de sobretasas
     * @return Valor del impuesto regional calculado
     */
    private double calcularImpuestoRegional(String region, double subtotal) {
        if (region == null || region.trim().isEmpty()) {
            return subtotal * 0.02; // Tasa por defecto general
        }
        switch (region.toUpperCase()) {
            case "BOGOTA":
            case "CUNDINAMARCA":
                return subtotal * 0.035;
            case "ANTIOQUIA":
                return subtotal * 0.030;
            case "VALLE":
                return subtotal * 0.028;
            case "ZONA_FRANCA":
                return 0.0;
            default:
                return subtotal * 0.020;
        }
    }

    /**
     * Intenta reenviar una notificación electrónica en caso de contingencia o fallo de red.
     *
     * @param email Buzón destinatario
     * @param mensaje Contenido del mensaje
     * @param intentosMaximos Tope de reintentos
     * @return true si la transmisión fue exitosa
     */
    private boolean reintentarNotificacion(String email, String mensaje, int intentosMaximos) {
        int intento = 1;
        while (intento <= intentosMaximos) {
            try {
                log.info("Intento de envio #{} para {}", intento, email);
                emailService.enviar(email, "Reintento de Notificacion", mensaje);
                return true;
            } catch (Exception ex) {
                log.warn("Fallo intento {} de envio a {}: {}", intento, email, ex.getMessage());
                intento++;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        log.error("Se agotaron los intentos de envio para el correo: {}", email);
        return false;
    }

    /**
     * Elimina registros de pedidos provisionales o caducados que no fueron completados.
     *
     * @param diasExpiracion Antigüedad en días para considerar un pedido vencido
     * @return Cantidad de registros purgados de la base de datos
     */
    private int purgarPedidosVencidos(int diasExpiracion) {
        log.info("Iniciando purga de pedidos con mas de {} dias de vencimiento", diasExpiracion);
        String sql = "DELETE FROM pedidos WHERE fecha < CURRENT_DATE - ? AND total = 0.0";
        try {
            int filasAfectadas = jdbcTemplate.update(sql, diasExpiracion);
            log.info("Total de pedidos obsoletos depurados: {}", filasAfectadas);
            return filasAfectadas;
        } catch (Exception e) {
            log.error("Error durante el proceso de depuracion de pedidos: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Construye la plantilla HTML/texto del correo de confirmación de pedido.
     *
     * @param pedidoId Numero de pedido confirmado
     * @param request Solicitud de pedido
     * @param total Monto final pagado
     * @param descuento Monto deducido por beneficios
     * @return Cadena formateada para el cuerpo del correo
     */
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

    /**
     * Metodo auxiliar para validacion interna de integridad de parametros.
     *
     * @param pedidoId Identificador a comprobar
     * @return true si el identificador es valido
     */
    private boolean validarConsistenciaInterna(Long pedidoId) {
        return pedidoId != null && pedidoId > 0;
    }
}
