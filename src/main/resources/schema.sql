CREATE TABLE IF NOT EXISTS clientes (
    id BIGINT PRIMARY KEY,
    tipo_cliente VARCHAR(50) NOT NULL,
    nit VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS productos (
    id BIGINT PRIMARY KEY,
    precio DOUBLE PRECISION NOT NULL
);

CREATE TABLE IF NOT EXISTS inventario (
    producto_id BIGINT PRIMARY KEY,
    cantidad INT NOT NULL,
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE TABLE IF NOT EXISTS facturas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    monto DOUBLE PRECISION NOT NULL,
    pagada BOOLEAN NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE IF NOT EXISTS pedidos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    total DOUBLE PRECISION NOT NULL,
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
);

CREATE TABLE IF NOT EXISTS detalle_pedido (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INT NOT NULL,
    precio_unitario DOUBLE PRECISION NOT NULL,
    FOREIGN KEY (pedido_id) REFERENCES pedidos(id),
    FOREIGN KEY (producto_id) REFERENCES productos(id)
);
