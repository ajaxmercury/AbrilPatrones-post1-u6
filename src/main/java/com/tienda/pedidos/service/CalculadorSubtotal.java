package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.ItemPedido;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Componente especializado en consultar precios del catalogo y calcular el subtotal.
 * Aislado para desacoplar el acceso SQL a productos de la logica de orquestacion.
 */
@Component
public class CalculadorSubtotal {

    private final JdbcTemplate jdbcTemplate;

    public CalculadorSubtotal(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Calcula el subtotal acumulado y recolecta los precios unitarios de cada item.
     *
     * @param items Lista de productos y cantidades solicitadas
     * @return Registro SubtotalDetalle con el subtotal monetario y mapa de precios
     */
    public SubtotalDetalle calcularSubtotal(List<ItemPedido> items) {
        double subtotal = 0.0;
        Map<Long, Double> precios = new HashMap<>();

        if (items != null) {
            for (ItemPedido item : items) {
                String sql = "SELECT precio FROM productos WHERE id = ?";
                List<Double> listaPrecios = jdbcTemplate.query(
                        sql,
                        (rs, rowNum) -> rs.getDouble("precio"),
                        item.getProductoId()
                );
                double precioUnitario = listaPrecios.isEmpty() ? 0.0 : listaPrecios.get(0);
                precios.put(item.getProductoId(), precioUnitario);
                subtotal += precioUnitario * item.getCantidad();
            }
        }
        return new SubtotalDetalle(subtotal, precios);
    }

    public record SubtotalDetalle(double subtotal, Map<Long, Double> preciosUnitarios) {}
}
