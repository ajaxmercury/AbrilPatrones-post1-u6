# Sistema de Gestión de Pedidos — Diagnóstico y Refactorización de Antipatrones

**Estudiante:** Abril  
**Materia:** Patrones de Diseño de Software — Unidad 6  
**Repositorio local y remoto:** `AbrilPatrones-post1-u6`  
**Descripción del proyecto:** Post-contenido — Diagnóstico y refactorización de antipatrones de diseño en el Sistema de Gestión de Pedidos (`pedidos-service`).  

---

## 1. Stack Tecnológico y Entorno

- **Lenguaje:** Java 17 (Ejecutado sobre OpenJDK 21.0.10 LTS Temurin con compatibilidad bytecode Java 17).
- **Framework:** Spring Boot 3.2.5.
- **Herramienta de construcción:** Apache Maven 3.9.11.
- **Persistencia en memoria:** H2 Database 2.2.x (`spring-boot-starter-jdbc` con `JdbcTemplate`).
- **Pruebas:** JUnit 5 (`spring-boot-starter-test`), `@SpringBootTest`, `@Transactional`.
- **Estructura de paquetes base:** `com.tienda.pedidos`
  - `dto/`: Objetos de transferencia de datos (`PedidoRequest`, `ItemPedido`, `ResultadoPedido`).
  - `service/`: Servicios de negocio, repositorios y orquestadores.
  - `validacion/`: Cadena de responsabilidad de validaciones de pedido.
  - `descuento/`: Estrategias de descuento por tipo de cliente y promociones.

---

## 2. PARTE 1 — Diagnóstico de Antipatrones en `GestorPedidos`

Antes de intervenir el código original, se realizó una inspección estática exhaustiva de la clase `GestorPedidos.java` ubicada en `src/main/java/com/tienda/pedidos/service/GestorPedidos.java` (335 líneas reales de código). A continuación se documenta la evidencia cuantitativa y cualitativa.

### 2.1. Evidencia Concreta de Responsabilidades Distintas y Rangos de Líneas Reales

La clase monolítica `GestorPedidos` concentra un total de **12 responsabilidades divergentes** (7 dentro del método principal `procesarPedido` y 5 distribuidas en métodos auxiliares no cohesivos):

| Bloque / Método | Rango de Líneas Reales | Responsabilidad Específica | Dependencia Acoplada |
| :--- | :---: | :--- | :--- |
| **Bloque 1** (`procesarPedido`) | `51` a `67` | Validación de existencia y stock suficiente en inventario | SQL embebido directo sobre tabla `inventario` |
| **Bloque 2** (`procesarPedido`) | `69` a `87` | Validación de cliente y política de corte de crédito por horario (mora antes de 20:00) | `JdbcTemplate` (tablas `clientes`, `facturas`) y `LocalTime.now()` |
| **Bloque 3** (`procesarPedido`) | `89` a `100` | Consulta de catálogo y cálculo de subtotal de productos | SQL embebido sobre tabla `productos` |
| **Bloque 4** (`procesarPedido`) | `102` a `129` | Cálculo de descuentos condicionales por tipo de cliente (`VIP`, `FRECUENTE`, `ESTANDAR`) | Consultas directas de conteo en tabla `pedidos` y bifurcaciones anidadas |
| **Bloque 5** (`procesarPedido`) | `131` a `137` | Liquidación fiscal de IVA (19%) y total final | Fórmulas aritméticas embebidas en el flujo |
| **Bloque 6** (`procesarPedido`) | `139` a `158` | Persistencia transaccional de orden, detalles y actualización de inventario | `INSERT INTO pedidos`, `CALL IDENTITY()`, `INSERT INTO detalle_pedido`, `UPDATE inventario` |
| **Bloque 7** (`procesarPedido`) | `160` a `166` | Formateo de mensaje y despacho de correo electrónico | `EmailService` y método de formateo |
| `obtenerHistorialCliente` | `180` a `202` | Consulta analítica de pedidos previos del cliente | SQL `SELECT id, total, fecha FROM pedidos` |
| `formatearFactura` | `213` a `226` | Representación tabular en texto de facturas de venta | Manipulación de cadenas y diseño de presentación |
| `calcularImpuestoRegional` | `235` a `252` | Reglas fiscales departamentales/territoriales | Casos condicionales (`switch`) por región geográfica |
| `reintentarNotificacion` | `262` a `282` | Gestión de contingencia de red y reintentos con latencia | `Thread.sleep` y manejo de concurrencia |
| `purgarPedidosVencidos` | `290` a `304` | Depuración y mantenimiento administrativo de registros en BD | SQL `DELETE FROM pedidos WHERE fecha < ...` |

### 2.2. Medición de Profundidad de Anidamiento

- **Lógica de Descuento (Líneas 102 a 129):** Posee una profundidad de anidamiento de **2 niveles de condicionales** (`if` exterior por tipo de cliente e `if-else if` anidados por umbrales de compra o conteo de pedidos históricos):
  - Nivel 1: `if ("VIP".equalsIgnoreCase(tipoCliente))`
    - Nivel 2: `if (subtotal > 1000000.0) ... else if (subtotal > 500000.0) ... else ...`
  - Nivel 1: `else if ("FRECUENTE".equalsIgnoreCase(tipoCliente))`
    - Nivel 2: `if (pedidosPrevios != null && pedidosPrevios > 10) ... else if (pedidosPrevios != null && pedidosPrevios > 3) ...`
- **Lógica de Mora y Corte Horario (Líneas 73 a 85):**
  - Nivel 1: `if (facturasPendientes != null && facturasPendientes > 0)`
    - Nivel 2: `if (ahora.isBefore(LocalTime.of(20, 0)))`

### 2.3. Niveles de Abstracción Simultáneos

El método `procesarPedido` rompe de forma crítica el principio de un solo nivel de abstracción por método (SLAP). En una misma función de 121 líneas interactúan simultáneamente **4 niveles de abstracción dispares**:
1. **Infraestructura y SQL Crudo (Nivel Bajo):** Manipulación de strings SQL (`"SELECT cantidad FROM inventario..."`, `"CALL IDENTITY()"`), mapeos manuales de tipos primitivos JDBC (`Integer.class`, `Double.class`).
2. **Acceso al Sistema y Tiempo Físico (Nivel Técnico):** Invocación acoplada a la hora de la máquina virtual con `LocalTime.now()` y llamadas a APIs de red/email.
3. **Reglas de Negocio Financieras y Políticas (Nivel Intermedio):** Reglas crediticias de mora condicionadas al horario de corte, escalas de descuento por fidelidad o estatus VIP, y tasa de IVA del 19%.
4. **Dominio y Orquestación Comercial (Nivel Alto):** Ciclo de vida general de una solicitud de pedido desde su recepción hasta la emisión del `ResultadoPedido` confirmado o rechazado.

### 2.4. Impacto al Agregar un Nuevo Tipo de Cliente (Violación de OCP)

Para incorporar un nuevo tipo de cliente (por ejemplo, `CORPORATIVO` con 10% de descuento directo si cuenta con NIT):
- Es imperativo abrir y modificar el método central `procesarPedido` en `GestorPedidos.java` entre las líneas 102 y 129.
- Se requeriría agregar entre **12 y 18 líneas de código condicional** adicionales (`else if ("CORPORATIVO".equalsIgnoreCase(tipoCliente)) { ... }`), además de consultar la columna `nit` en la base de datos.
- **Riesgo:** Tocar este método central pone en riesgo el funcionamiento de los tipos de cliente existentes (`VIP`, `FRECUENTE`, `ESTANDAR`), no permite compilar estrategias de forma independiente, e incrementa la fragilidad del sistema.

### 2.5. Identificación de Antipatrones

1. **God Object (Blob / Objeto Todopoderoso):**
   `GestorPedidos` asume responsabilidades que deberían estar distribuidas en repositorios, validadores, motores de cálculo fiscal, servicios de notificación y rutinas de mantenimiento de base de datos. Posee alto acoplamiento y nula cohesión. No es posible probar de forma aislada una regla de negocio sin levantar la infraestructura JDBC ni interactuar con la base de datos.
2. **Spaghetti Code (Código Espagueti):**
   El flujo de control dentro de `procesarPedido` es una maraña lineal de bifurcaciones anidadas, accesos a base de datos intercalados con operaciones aritméticas y efectos colaterales (inserciones y actualizaciones intermedias). La lógica no puede reutilizarse ni mantenerse de forma modular.

---

## 3. Pruebas de la Línea Base y Tabla de Salida Original

Se ejecutó la suite de pruebas unitarias y de integración `GestorPedidosTest.java` sobre la implementación original monolítica. 

### Tabla de Salida Original (`docs/salida-original.txt`):

```text
CASO                           | CONFIRMADO   | MOTIVO                                        | TOTAL       
---------------------------------------------------------------------------------------------------------
CLIENTE_INEXISTENTE            | false        | EmptyResultDataAccessException                | 0.00        
CLIENTE_MOROSO_HORA_REAL       | false        | Cliente con facturas pendientes               | 0.00        
DESCUENTO_FRECUENTE            | true         | N/A                                           | 437920.00   
DESCUENTO_VIP                  | true         | N/A                                           | 1213800.00  
STOCK_INSUFICIENTE             | false        | Stock insuficiente: producto 104              | 0.00        
```

*Nota sobre la hora de corte:* La prueba se ejecutó a las 14:39 (hora local del sistema), la cual es anterior al horario de corte de las 20:00 (`LocalTime.now().isBefore(LocalTime.of(20, 0))` evaluó a `true`), confirmando el rechazo del cliente moroso.
