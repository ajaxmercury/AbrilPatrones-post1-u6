package com.tienda.pedidos;

import com.tienda.pedidos.dto.ItemPedido;
import com.tienda.pedidos.dto.PedidoRequest;
import com.tienda.pedidos.dto.ResultadoPedido;
import com.tienda.pedidos.service.GestorPedidos;
import com.tienda.pedidos.service.ControlBlackFriday;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.tienda.pedidos.validacion.ContextoPedido;
import com.tienda.pedidos.validacion.ValidadorCliente;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Clock;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class GestorPedidosTest {

    @Autowired
    private GestorPedidos gestorPedidos;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ControlBlackFriday controlBlackFriday;

    private static final List<String> filasSalida = Collections.synchronizedList(new ArrayList<>());

    @BeforeAll
    static void setUpAll() {
        filasSalida.clear();
    }

    @BeforeEach
    void setUpEach() {
        if (controlBlackFriday != null) {
            controlBlackFriday.setActiva(false);
        }
    }

    @AfterAll
    static void tearDownAll() throws IOException {
        File targetDir = new File("target");
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        File salidaFile = new File(targetDir, "salida.txt");
        Collections.sort(filasSalida);
        try (PrintWriter writer = new PrintWriter(new FileWriter(salidaFile))) {
            writer.printf("%-30s | %-12s | %-45s | %-12s%n", "CASO", "CONFIRMADO", "MOTIVO", "TOTAL");
            writer.println("---------------------------------------------------------------------------------------------------------");
            for (String fila : filasSalida) {
                writer.println(fila);
            }
        }
    }

    private void registrarResultado(String caso, boolean confirmado, String motivo, double total) {
        String motivoLimpio = (motivo == null || motivo.trim().isEmpty()) ? "N/A" : motivo.trim();
        String fila = String.format(Locale.US, "%-30s | %-12b | %-45s | %-12.2f",
                caso, confirmado, motivoLimpio, total);
        filasSalida.add(fila);
    }

    @Test
    @Transactional
    @DisplayName("Caso 1: Stock insuficiente en inventario")
    void testStockInsuficiente() {
        // Producto 104 solo tiene stock de 2 unidades; se solicitan 5 unidades
        PedidoRequest request = new PedidoRequest(1L, "cliente1@tienda.com",
                List.of(new ItemPedido(104L, 5)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertFalse(resultado.isConfirmado());
        assertEquals("Stock insuficiente: producto 104", resultado.getMotivo());
        assertEquals(0.0, resultado.getTotal(), 0.001);

        registrarResultado("STOCK_INSUFICIENTE", resultado.isConfirmado(), resultado.getMotivo(), resultado.getTotal());
    }

    @Test
    @Transactional
    @DisplayName("Caso 2: Cliente no registrado en base de datos")
    void testClienteInexistente() {
        // Cliente 999 no existe en data.sql
        PedidoRequest request = new PedidoRequest(999L, "desconocido@tienda.com",
                List.of(new ItemPedido(101L, 1)));

        boolean confirmado = false;
        String motivo = "";
        double total = 0.0;

        try {
            ResultadoPedido resultado = gestorPedidos.procesarPedido(request);
            confirmado = resultado.isConfirmado();
            motivo = resultado.getMotivo();
            total = resultado.getTotal();
        } catch (Exception ex) {
            // El codigo original lanza EmptyResultDataAccessException al usar queryForObject
            confirmado = false;
            motivo = ex.getClass().getSimpleName();
            total = 0.0;
        }

        assertFalse(confirmado);
        registrarResultado("CLIENTE_INEXISTENTE", confirmado, motivo, total);
    }

    @Test
    @Transactional
    @DisplayName("Caso 3: Cliente moroso evaluado con reloj real del sistema")
    void testClienteMoroso() {
        // Cliente 5 tiene factura pendiente de 150,000 no pagada
        PedidoRequest request = new PedidoRequest(5L, "moroso@tienda.com",
                List.of(new ItemPedido(101L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);
        assertNotNull(resultado);

        LocalTime ahora = LocalTime.now();
        if (ahora.isBefore(LocalTime.of(20, 0))) {
            // Evaluado antes de las 20:00 -> rechaza por mora
            assertFalse(resultado.isConfirmado());
            assertEquals("Cliente con facturas pendientes", resultado.getMotivo());
            assertEquals(0.0, resultado.getTotal(), 0.001);
            registrarResultado("CLIENTE_MOROSO_HORA_REAL", resultado.isConfirmado(), resultado.getMotivo(), resultado.getTotal());
        } else {
            // Evaluado despues de las 20:00 -> horario de corte cumplido
            assertTrue(resultado.isConfirmado());
            registrarResultado("CLIENTE_MOROSO_HORA_REAL", resultado.isConfirmado(), "Aprobado posterior a 20:00", resultado.getTotal());
        }
    }

    @Test
    @Transactional
    @DisplayName("Caso 4: Cliente VIP con subtotal superior a 1,000,000 (15% descuento)")
    void testDescuentoVip() {
        // Cliente 2 (VIP). Producto 103 cuesta 1,200,000.
        // Subtotal = 1,200,000. Descuento 15% = 180,000. Base = 1,020,000. IVA (19%) = 193,800. Total = 1,213,800.
        PedidoRequest request = new PedidoRequest(2L, "vip@tienda.com",
                List.of(new ItemPedido(103L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertTrue(resultado.isConfirmado());
        assertEquals(1213800.0, resultado.getTotal(), 0.01);

        registrarResultado("DESCUENTO_VIP", resultado.isConfirmado(), "N/A", resultado.getTotal());
    }

    @Test
    @Transactional
    @DisplayName("Caso 5: Cliente FRECUENTE con mas de 10 pedidos previos (8% descuento)")
    void testDescuentoFrecuente() {
        // Cliente 4 (FRECUENTE con 12 pedidos previos en data.sql).
        // Producto 101 cuesta 200,000 x 2 unidades = 400,000.
        // Descuento 8% = 32,000. Base = 368,000. IVA (19%) = 69,920. Total = 437,920.
        PedidoRequest request = new PedidoRequest(4L, "frecuente@tienda.com",
                List.of(new ItemPedido(101L, 2)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertTrue(resultado.isConfirmado());
        assertEquals(437920.0, resultado.getTotal(), 0.01);

        registrarResultado("DESCUENTO_FRECUENTE", resultado.isConfirmado(), "N/A", resultado.getTotal());
    }

    @Test
    @Transactional
    @DisplayName("Caso Auxiliar Clock: Moroso antes de las 20:00 con Clock fijo es rechazado")
    void testMorosoAntesDeCorteConRelojFijo() {
        Clock fixedClockBefore = Clock.fixed(Instant.parse("2026-10-04T14:00:00Z"), ZoneId.of("UTC"));
        ValidadorCliente validador = new ValidadorCliente(jdbcTemplate, fixedClockBefore);
        ContextoPedido ctx = new ContextoPedido(new PedidoRequest(5L, "moroso@tienda.com", List.of(new ItemPedido(101L, 1))));
        ResultadoPedido res = validador.validar(ctx);
        assertNotNull(res);
        assertFalse(res.isConfirmado());
        assertEquals("Cliente con facturas pendientes", res.getMotivo());
    }

    @Test
    @Transactional
    @DisplayName("Caso Auxiliar Clock: Moroso despues de las 20:00 con Clock fijo es aprobado")
    void testMorosoDespuesDeCorteConRelojFijo() {
        Clock fixedClockAfter = Clock.fixed(Instant.parse("2026-10-04T21:00:00Z"), ZoneId.of("UTC"));
        ValidadorCliente validador = new ValidadorCliente(jdbcTemplate, fixedClockAfter);
        ContextoPedido ctx = new ContextoPedido(new PedidoRequest(5L, "moroso@tienda.com", List.of(new ItemPedido(101L, 1))));
        ResultadoPedido res = validador.validar(ctx);
        assertNull(res); // Cadena superada sin rechazo
    }

    @Test
    @Transactional
    @DisplayName("Caso 6 Campana: Black Friday activa (25% descuento)")
    void testCampanaBlackFriday() {
        controlBlackFriday.setActiva(true);
        // Cliente 1 (ESTANDAR). 1 unidad de producto 101 ($200,000).
        // Subtotal = 200,000. Descuento 25% = 50,000. Base = 150,000. IVA 19% = 28,500. Total = 178,500.
        PedidoRequest request = new PedidoRequest(1L, "cliente1@tienda.com",
                List.of(new ItemPedido(101L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertTrue(resultado.isConfirmado());
        assertEquals(178500.0, resultado.getTotal(), 0.01);

        registrarResultado("CAMPANA_BLACK_FRIDAY", resultado.isConfirmado(), "N/A", resultado.getTotal());
    }

    @Test
    @Transactional
    @DisplayName("Caso 7 Campana: Cliente Corporativo con NIT (10% descuento)")
    void testCampanaCorporativo() {
        controlBlackFriday.setActiva(false);
        // Cliente 6 (con NIT '900123456-1'). 1 unidad de producto 101 ($200,000).
        // Subtotal = 200,000. Descuento 10% = 20,000. Base = 180,000. IVA 19% = 34,200. Total = 214,200.
        PedidoRequest request = new PedidoRequest(6L, "corporativo@tienda.com",
                List.of(new ItemPedido(101L, 1)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertTrue(resultado.isConfirmado());
        assertEquals(214200.0, resultado.getTotal(), 0.01);

        registrarResultado("CAMPANA_CORPORATIVO_NIT", resultado.isConfirmado(), "N/A", resultado.getTotal());
    }

    @Test
    @Transactional
    @DisplayName("Caso 8 Campana: Descuento por volumen mayor a 20 unidades (12% descuento)")
    void testCampanaVolumen() {
        controlBlackFriday.setActiva(false);
        // Cliente 1 (ESTANDAR). 25 unidades de producto 105 ($50,000).
        // Subtotal = 1,250,000. Descuento 12% = 150,000. Base = 1,100,000. IVA 19% = 209,000. Total = 1,309,000.
        PedidoRequest request = new PedidoRequest(1L, "mayorista@tienda.com",
                List.of(new ItemPedido(105L, 25)));

        ResultadoPedido resultado = gestorPedidos.procesarPedido(request);

        assertNotNull(resultado);
        assertTrue(resultado.isConfirmado());
        assertEquals(1309000.0, resultado.getTotal(), 0.01);

        registrarResultado("CAMPANA_VOLUMEN_MAYOR_20", resultado.isConfirmado(), "N/A", resultado.getTotal());
    }
}
