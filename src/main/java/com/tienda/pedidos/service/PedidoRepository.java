package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Repositorio de persistencia relacional para pedidos y detalles.
 * Emplea GeneratedKeyHolder para obtener las claves primarias autogeneradas,
 * eliminando la dependencia fragil de funciones propietarias como CALL IDENTITY().
 */
@Repository
public class PedidoRepository {

    private static final Logger log = LoggerFactory.getLogger(PedidoRepository.class);
    private final JdbcTemplate jdbcTemplate;

    public PedidoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Persiste la cabecera del pedido, sus items asociados y descuenta las existencias de inventario.
     *
     * @param clienteId Identificador del cliente comprador
     * @param total Monto total liquidado con impuestos
     * @param items Lista de productos y cantidades
     * @param preciosUnitarios Mapa de precios unitarios vigentes
     * @return Identificador unico generado para el pedido
     */
    @Transactional
    public Long guardarPedido(Long clienteId, double total, List<ItemPedido> items, Map<Long, Double> preciosUnitarios) {
        String sqlPedido = "INSERT INTO pedidos (cliente_id, total, fecha) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sqlPedido, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, clienteId);
            ps.setDouble(2, total);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("No se pudo obtener el ID autogenerado para el pedido");
        }
        Long pedidoId = key.longValue();

        String sqlDetalle = "INSERT INTO detalle_pedido (pedido_id, producto_id, cantidad, precio_unitario) VALUES (?, ?, ?, ?)";
        String sqlStock = "UPDATE inventario SET cantidad = cantidad - ? WHERE producto_id = ?";

        for (ItemPedido item : items) {
            double precio = preciosUnitarios.getOrDefault(item.getProductoId(), 0.0);
            jdbcTemplate.update(sqlDetalle, pedidoId, item.getProductoId(), item.getCantidad(), precio);
            jdbcTemplate.update(sqlStock, item.getCantidad(), item.getProductoId());
        }

        log.info("Pedido #{} y sus {} items fueron persistidos exitosamente", pedidoId, items.size());
        return pedidoId;
    }
}
