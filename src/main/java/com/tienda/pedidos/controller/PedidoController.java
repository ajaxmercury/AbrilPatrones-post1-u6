package com.tienda.pedidos.controller;

import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador REST para la recepcion y gestion de pedidos comerciales.
 */
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final GestorPedidos gestorPedidos;

    public PedidoController(GestorPedidos gestorPedidos) {
        this.gestorPedidos = gestorPedidos;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "servicio", "pedidos-service",
                "version", "1.0.0-REFACTORED",
                "arquitectura", "Clean Architecture - Chain of Responsibility + Strategy"
        ));
    }

    @PostMapping
    public ResponseEntity<ResultadoPedido> procesarPedido(@RequestBody PedidoRequest request) {
        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);
        if (resultado.isConfirmado()) {
            return ResponseEntity.ok(resultado);
        } else {
            return ResponseEntity.badRequest().body(resultado);
        }
    }
}
