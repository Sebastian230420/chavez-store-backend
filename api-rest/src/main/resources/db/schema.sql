-- ═══════════════════════════════════════════════════════════════════
--  Chavez Store — Esquema de base de datos
--  MySQL 8+ / 9+ · InnoDB · utf8mb4
--  Verificado ejecutando sobre MySQL 9.3.0
--
--  Aplicar manualmente:
--    mysql -u root -p < src/main/resources/db/schema.sql
--
--  Spring Boot NO lo ejecuta solo (spring.sql.init.mode=never) para no
--  borrar datos en cada arranque. Aplica el archivo a mano.
-- ═══════════════════════════════════════════════════════════════════

CREATE DATABASE IF NOT EXISTS chavez_store
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE chavez_store;

-- ═══════════════════════════════════════════════════════════════════
--  SEGURIDAD
-- ═══════════════════════════════════════════════════════════════════
DROP TABLE IF EXISTS user_roles;
DROP TABLE IF EXISTS abonos;
DROP TABLE IF EXISTS sale_details;
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS stock_movements;
DROP TABLE IF EXISTS lots;
DROP TABLE IF EXISTS purchase_details;
DROP TABLE IF EXISTS purchases;
DROP TABLE IF EXISTS suppliers;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS product_presentations;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS brands;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;

CREATE TABLE roles (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(30)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_roles_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE users (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)   NOT NULL,
    email           VARCHAR(100)  NOT NULL,
    password_hash   VARCHAR(255)  NOT NULL,
    full_name       VARCHAR(150)  NOT NULL,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    failed_attempts INT           NOT NULL DEFAULT 0,
    locked_until    DATETIME      NULL,
    last_login_at   DATETIME      NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)    ON DELETE CASCADE
) ENGINE=InnoDB;

-- ═══════════════════════════════════════════════════════════════════
--  CATÁLOGO
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE brands (
    id     BIGINT      NOT NULL AUTO_INCREMENT,
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uq_brands_name UNIQUE (name)
) ENGINE=InnoDB;

CREATE TABLE products (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    sku         VARCHAR(50)    NOT NULL,
    barcode     VARCHAR(50)    NULL,
    name        VARCHAR(150)   NOT NULL,
    description TEXT           NULL,
    category_id BIGINT         NOT NULL,
    brand_id    BIGINT         NULL,
    base_unit   ENUM('UNIDAD','LT','ML') NOT NULL DEFAULT 'UNIDAD',
    content_ml  INT            NULL,
    min_stock   INT            NOT NULL DEFAULT 0,
    max_stock   INT            NULL,
    stock       INT            NOT NULL DEFAULT 0,
    cost_avg    DECIMAL(12,4)  NOT NULL DEFAULT 0.0000,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_products_sku     UNIQUE (sku),
    CONSTRAINT uq_products_barcode UNIQUE (barcode),
    CONSTRAINT fk_pr_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_pr_brand    FOREIGN KEY (brand_id)    REFERENCES brands(id),
    CONSTRAINT ck_pr_stock_nonneg CHECK (stock >= 0),
    INDEX idx_pr_category (category_id),
    INDEX idx_pr_active (active),
    INDEX idx_pr_name (name)
) ENGINE=InnoDB;

-- Presentaciones: factor de conversión a unidad base
CREATE TABLE product_presentations (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    product_id  BIGINT         NOT NULL,
    name        VARCHAR(60)    NOT NULL,
    units_base  INT            NOT NULL,
    type        ENUM('COMPRA','VENTA') NOT NULL,
    price       DECIMAL(12,2)  NOT NULL DEFAULT 0.00,
    stock_min   INT            NULL,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    sort_order  INT            NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_pp_product_name_type UNIQUE (product_id, name, type),
    CONSTRAINT fk_pp_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT ck_pp_units_positive CHECK (units_base > 0)
) ENGINE=InnoDB;

-- ═══════════════════════════════════════════════════════════════════
--  PROVEEDORES Y COMPRAS
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE suppliers (
    id       BIGINT        NOT NULL AUTO_INCREMENT,
    document VARCHAR(20)   NOT NULL,
    name     VARCHAR(150)  NOT NULL,
    phone    VARCHAR(30)   NULL,
    email    VARCHAR(100)  NULL,
    address  VARCHAR(200)  NULL,
    active   BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uq_suppliers_document UNIQUE (document)
) ENGINE=InnoDB;

CREATE TABLE purchases (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    supplier_id BIGINT        NOT NULL,
    document    VARCHAR(20)   NOT NULL,
    issue_date  DATE          NOT NULL,
    status      ENUM('REGISTRADA','RECIBIDA','ANULADA') NOT NULL DEFAULT 'REGISTRADA',
    total       DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    notes       TEXT          NULL,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_purchases_document UNIQUE (document),
    CONSTRAINT fk_pu_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    INDEX idx_pu_status (status),
    INDEX idx_pu_date (issue_date)
) ENGINE=InnoDB;

CREATE TABLE purchase_details (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    purchase_id     BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    presentation_id BIGINT        NOT NULL,
    qty_bought      INT           NOT NULL,
    qty_received    INT           NOT NULL DEFAULT 0,
    units_base      INT           NOT NULL,
    unit_cost       DECIMAL(12,4) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    -- Vencimiento capturado al registrar; se usa al crear el lote al recibir
    expiry_date     DATE          NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pd_purchase     FOREIGN KEY (purchase_id)     REFERENCES purchases(id) ON DELETE CASCADE,
    CONSTRAINT fk_pd_product      FOREIGN KEY (product_id)      REFERENCES products(id),
    CONSTRAINT fk_pd_presentation FOREIGN KEY (presentation_id) REFERENCES product_presentations(id),
    CONSTRAINT ck_pd_qty_bought   CHECK (qty_bought > 0),
    CONSTRAINT ck_pd_qty_received CHECK (qty_received >= 0)
) ENGINE=InnoDB;

-- ═══════════════════════════════════════════════════════════════════
--  LOTES — vencimiento y salida FEFO
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE lots (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    lot_code           VARCHAR(60)   NOT NULL,
    product_id         BIGINT        NOT NULL,
    purchase_detail_id BIGINT        NULL,
    entry_date         DATE          NOT NULL,
    expiry_date        DATE          NULL,
    qty_received       INT           NOT NULL,
    qty_remaining      INT           NOT NULL,
    cost_unit          DECIMAL(12,4) NOT NULL,
    active             BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uq_lots_code UNIQUE (lot_code),
    CONSTRAINT fk_lo_product FOREIGN KEY (product_id)
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_lo_pd      FOREIGN KEY (purchase_detail_id)
        REFERENCES purchase_details(id) ON DELETE SET NULL,
    CONSTRAINT ck_lo_qty_rem CHECK (qty_remaining >= 0),
    INDEX idx_lo_fefo (product_id, expiry_date, qty_remaining)
) ENGINE=InnoDB;

-- ═══════════════════════════════════════════════════════════════════
--  KARDEX — fuente de verdad del stock (append only)
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE stock_movements (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    product_id      BIGINT        NOT NULL,
    lot_id          BIGINT        NULL,
    presentation_id BIGINT        NULL,
    type            ENUM(
        'INVENTARIO_INICIAL','COMPRA','VENTA','AJUSTE_POSITIVO',
        'AJUSTE_NEGATIVO','MERMA','DEVOLUCION_PROVEEDOR') NOT NULL,
    qty             INT           NOT NULL,
    unit_cost       DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
    ref_table       VARCHAR(50)   NULL,
    ref_id          BIGINT        NULL,
    reason          VARCHAR(255)  NULL,
    user_id         BIGINT        NOT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_sm_product FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_sm_lot     FOREIGN KEY (lot_id)     REFERENCES lots(id),
    CONSTRAINT fk_sm_user    FOREIGN KEY (user_id)    REFERENCES users(id),
    CONSTRAINT ck_sm_reason CHECK (
        type NOT IN ('MERMA','AJUSTE_POSITIVO','AJUSTE_NEGATIVO')
        OR reason IS NOT NULL
    ),
    INDEX idx_sm_product_date (product_id, created_at),
    INDEX idx_sm_type_date (type, created_at),
    INDEX idx_sm_ref (ref_table, ref_id)
) ENGINE=InnoDB;

-- ═══════════════════════════════════════════════════════════════════
--  CLIENTES
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE customers (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    document     VARCHAR(20)   NOT NULL,
    type         ENUM('CONSUMIDOR','MAYORISTA') NOT NULL DEFAULT 'CONSUMIDOR',
    full_name    VARCHAR(150)  NOT NULL,
    phone        VARCHAR(30)   NULL,
    email        VARCHAR(100)  NULL,
    address      VARCHAR(200)  NULL,
    credit_limit DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_customers_document UNIQUE (document),
    INDEX idx_cu_active (active),
    INDEX idx_cu_name (full_name)
) ENGINE=InnoDB;

-- ═══════════════════════════════════════════════════════════════════
--  VENTAS — registro interno (sin caja, sin cobro, sin voucher)
-- ═══════════════════════════════════════════════════════════════════
CREATE TABLE sales (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    document        VARCHAR(20)   NOT NULL,
    customer_id     BIGINT        NULL,
    user_id         BIGINT        NOT NULL,
    sale_date       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type            ENUM('MOSTRADOR','CREDITO') NOT NULL DEFAULT 'MOSTRADOR',
    subtotal        DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total           DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    cost_total      DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    profit          DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status          ENUM('PAGADA','ANULADA') NOT NULL DEFAULT 'PAGADA',
    annulled_at     DATETIME      NULL,
    annul_reason    VARCHAR(255)  NULL,
    annul_user_id   BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_sales_document UNIQUE (document),
    CONSTRAINT fk_sa_customer FOREIGN KEY (customer_id)   REFERENCES customers(id),
    CONSTRAINT fk_sa_user     FOREIGN KEY (user_id)       REFERENCES users(id),
    CONSTRAINT fk_sa_annul    FOREIGN KEY (annul_user_id) REFERENCES users(id),
    INDEX idx_sa_date_status (sale_date, status),
    INDEX idx_sa_customer (customer_id)
) ENGINE=InnoDB;

CREATE TABLE sale_details (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    sale_id         BIGINT        NOT NULL,
    product_id      BIGINT        NOT NULL,
    presentation_id BIGINT        NOT NULL,
    lot_id          BIGINT        NULL,
    qty             INT           NOT NULL,
    units_base      INT           NOT NULL,
    unit_price      DECIMAL(12,2) NOT NULL,
    unit_cost       DECIMAL(12,4) NOT NULL,
    subtotal        DECIMAL(12,2) NOT NULL,
    profit          DECIMAL(12,2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_sd_sale         FOREIGN KEY (sale_id)         REFERENCES sales(id) ON DELETE CASCADE,
    CONSTRAINT fk_sd_product      FOREIGN KEY (product_id)      REFERENCES products(id),
    CONSTRAINT fk_sd_presentation FOREIGN KEY (presentation_id) REFERENCES product_presentations(id),
    CONSTRAINT fk_sd_lot          FOREIGN KEY (lot_id)          REFERENCES lots(id),
    CONSTRAINT ck_sd_qty_positive CHECK (qty > 0),
    INDEX idx_sd_product (product_id),
    INDEX idx_sd_sale (sale_id)
) ENGINE=InnoDB;

-- ABONOS: única fuente de cobranza (solo clientes con crédito)
CREATE TABLE abonos (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    customer_id     BIGINT        NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    method          ENUM('EFECTIVO','TARJETA_DEBITO','TARJETA_CREDITO',
                         'YAPE','PLIN','TRANSFERENCIA') NOT NULL,
    applied_sale_id BIGINT        NULL,
    notes           VARCHAR(255)  NULL,
    user_id         BIGINT        NOT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    annulled_at     DATETIME      NULL,
    annul_user_id   BIGINT        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_ab_customer FOREIGN KEY (customer_id)   REFERENCES customers(id),
    CONSTRAINT fk_ab_sale     FOREIGN KEY (applied_sale_id) REFERENCES sales(id) ON DELETE SET NULL,
    CONSTRAINT fk_ab_user     FOREIGN KEY (user_id)       REFERENCES users(id),
    CONSTRAINT fk_ab_annul    FOREIGN KEY (annul_user_id) REFERENCES users(id),
    CONSTRAINT ck_ab_amount_positive CHECK (amount > 0),
    INDEX idx_ab_customer (customer_id),
    INDEX idx_ab_date (created_at)
) ENGINE=InnoDB;

-- ═══════════════════════════════════════════════════════════════════
--  DATOS INICIALES
-- ═══════════════════════════════════════════════════════════════════
INSERT INTO roles (name) VALUES
    ('ADMIN'), ('SUPERVISOR'), ('CAJERO'), ('ALMACENERO');

INSERT INTO categories (name) VALUES
    ('Cervezas'), ('Gaseosas'), ('Aguas'), ('Energizantes'),
    ('Bebidas isotónicas'), ('Licores'), ('Vinos'), ('Otros');

INSERT INTO brands (name) VALUES
    ('Cristal'), ('Cusqueña'), ('Backus'), ('Corona'), ('Heineken'),
    ('Coca-Cola'), ('Pepsi'), ('Inca Kola'), ('Fanta'), ('Sprite'),
    ('Red Bull'), ('Agua Tonic'), ('Bitters');

-- Usuario admin inicial
--   username: admin
--   password: Admin123   (hash BCrypt verificado, 10 rounds)
-- IMPORTANTE: cambiar este password antes de usar el sistema en produccion.
INSERT INTO users (username, email, password_hash, full_name) VALUES
    ('admin', 'admin@chavezstore.local',
     '$2a$10$Aym.RzWAwmzlEUCs4zexUezdRXLADbzN9vFGD3Li4d9CAKns/G7eS',
     'Administrador');

INSERT INTO user_roles (user_id, role_id)
    SELECT u.id, r.id FROM users u, roles r
    WHERE u.username = 'admin' AND r.name = 'ADMIN';