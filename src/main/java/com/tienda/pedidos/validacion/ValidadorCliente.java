package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ResultadoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalTime;
import java.util.List;

/**
 * Segundo eslabon de la cadena: Verifica el registro del cliente, su solvencia financiera
 * y la politica de corte horario (rechaza deudas si la hora es anterior a las 20:00).
 * Carga ademas los metadatos comerciales del cliente en el ContextoPedido.
 */
@Component
public class ValidadorCliente extends ValidadorPedido {

    private static final Logger log = LoggerFactory.getLogger(ValidadorCliente.class);

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public ValidadorCliente(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    protected ResultadoPedido ejecutarValidacion(ContextoPedido contexto) {
        Long clienteId = contexto.getRequest().getClienteId();

        // Consulta segura de cliente para evitar EmptyResultDataAccessException
        String sqlCliente = "SELECT tipo_cliente, nit FROM clientes WHERE id = ?";
        List<ClienteInfo> clientes = jdbcTemplate.query(
                sqlCliente,
                (rs, rowNum) -> new ClienteInfo(rs.getString("tipo_cliente"), rs.getString("nit")),
                clienteId
        );

        if (clientes.isEmpty()) {
            log.warn("Cliente ID: {} no se encuentra registrado en el sistema", clienteId);
            return ResultadoPedido.rechazado("Cliente no registrado");
        }

        ClienteInfo clienteInfo = clientes.get(0);
        contexto.setTipoCliente(clienteInfo.tipoCliente());
        contexto.setNit(clienteInfo.nit());

        // Carga de historial de pedidos previos para calculo de fidelidad
        String sqlPedidos = "SELECT COUNT(*) FROM pedidos WHERE cliente_id = ?";
        Integer pedidosPrevios = jdbcTemplate.queryForObject(sqlPedidos, Integer.class, clienteId);
        contexto.setPedidosPrevios(pedidosPrevios != null ? pedidosPrevios : 0);

        // Evaluacion de mora financiera y corte de horario
        String sqlFacturas = "SELECT COUNT(*) FROM facturas WHERE cliente_id = ? AND pagada = false";
        Integer facturasPendientes = jdbcTemplate.queryForObject(sqlFacturas, Integer.class, clienteId);

        if (facturasPendientes != null && facturasPendientes > 0) {
            LocalTime ahora = LocalTime.now(clock);
            log.debug("Evaluando mora crediticia para cliente {} a las {}", clienteId, ahora);
            if (ahora.isBefore(LocalTime.of(20, 0))) {
                log.warn("Cliente {} presenta facturas pendientes antes del horario de corte (20:00)", clienteId);
                return ResultadoPedido.rechazado("Cliente con facturas pendientes");
            }
        }

        return null;
    }

    private record ClienteInfo(String tipoCliente, String nit) {}
}
