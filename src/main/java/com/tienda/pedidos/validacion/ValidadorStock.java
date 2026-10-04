package com.tienda.pedidos.validacion;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.ResultadoPedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Primer eslabon de la cadena: Verifica la disponibilidad fisica en inventario.
 * Si no hay stock suficiente o el producto no existe en inventario, rechaza de inmediato.
 */
@Component
public class ValidadorStock extends ValidadorPedido {

    private static final Logger log = LoggerFactory.getLogger(ValidadorStock.class);
    private final JdbcTemplate jdbcTemplate;

    public ValidadorStock(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    protected ResultadoPedido ejecutarValidacion(ContextoPedido contexto) {
        List<ItemPedido> items = contexto.getRequest().getItems();
        if (items == null || items.isEmpty()) {
            return ResultadoPedido.rechazado("El pedido no contiene productos");
        }

        for (ItemPedido item : items) {
            String sql = "SELECT cantidad FROM inventario WHERE producto_id = ?";
            List<Integer> cantidades = jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> rs.getInt("cantidad"),
                    item.getProductoId()
            );

            if (cantidades.isEmpty() || cantidades.get(0) < item.getCantidad()) {
                log.warn("Stock insuficiente o inexistente para producto ID: {}", item.getProductoId());
                return ResultadoPedido.rechazado("Stock insuficiente: producto " + item.getProductoId());
            }
        }
        return null;
    }
}
