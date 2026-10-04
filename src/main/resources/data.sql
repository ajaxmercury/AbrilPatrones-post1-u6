-- Clientes
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (1, 'ESTANDAR', NULL);
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (2, 'VIP', NULL);
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (3, 'FRECUENTE', NULL);
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (4, 'FRECUENTE', NULL);
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (5, 'ESTANDAR', NULL);
INSERT INTO clientes (id, tipo_cliente, nit) VALUES (6, 'ESTANDAR', '900123456-1');

-- Productos
INSERT INTO productos (id, precio) VALUES (101, 200000.0);
INSERT INTO productos (id, precio) VALUES (102, 600000.0);
INSERT INTO productos (id, precio) VALUES (103, 1200000.0);
INSERT INTO productos (id, precio) VALUES (104, 10000.0);
INSERT INTO productos (id, precio) VALUES (105, 50000.0);

-- Inventario
INSERT INTO inventario (producto_id, cantidad) VALUES (101, 50);
INSERT INTO inventario (producto_id, cantidad) VALUES (102, 10);
INSERT INTO inventario (producto_id, cantidad) VALUES (103, 5);
INSERT INTO inventario (producto_id, cantidad) VALUES (104, 2);
INSERT INTO inventario (producto_id, cantidad) VALUES (105, 100);

-- Facturas
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (1, 80000.0, true);
INSERT INTO facturas (cliente_id, monto, pagada) VALUES (5, 150000.0, false);

-- Historial de pedidos previos para clientes frecuentes
-- Cliente 3: 5 pedidos previos (>3 y <=10)
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (3, 50000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (3, 60000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (3, 70000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (3, 80000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (3, 90000.0, CURRENT_TIMESTAMP);

-- Cliente 4: 12 pedidos previos (>10)
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
INSERT INTO pedidos (cliente_id, total, fecha) VALUES (4, 45000.0, CURRENT_TIMESTAMP);
