-- =============================================
-- REVSHOP P2 FINAL DATABASE SCHEMA
-- Compatible with ORACLE 10g XE / 11g / 12c+
-- =============================================

-- =========================
-- DROP TABLES (SAFE RESET)
-- =========================

BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE password_recovery_tokens CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE notification CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE favorites CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE review CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE payments CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE order_item CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE orders CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE cart_item CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE carts CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE addresses CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE product CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE sellers CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP TABLE users CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- =========================
-- DROP SEQUENCES
-- =========================

BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE users_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE seller_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE product_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE address_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE cart_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE cart_item_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE orders_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE order_item_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE payment_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE reviews_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE favorite_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE notification_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN
   EXECUTE IMMEDIATE 'DROP SEQUENCE password_recovery_seq';
EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- =========================
-- USERS
-- =========================

CREATE TABLE users (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    email VARCHAR2(150) UNIQUE NOT NULL,
    password VARCHAR2(255) NOT NULL,
    phone VARCHAR2(20),
    role VARCHAR2(50) NOT NULL,
    enabled NUMBER(1) DEFAULT 1,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER users_trg
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT users_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- =========================
-- SELLERS
-- =========================

CREATE TABLE sellers (
    id NUMBER PRIMARY KEY,
    user_id NUMBER UNIQUE NOT NULL,
    business_name VARCHAR2(150) NOT NULL,
    business_details VARCHAR2(2000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_sellers_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE SEQUENCE seller_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER sellers_trg
BEFORE INSERT ON sellers
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT seller_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- =========================
-- PRODUCT
-- =========================

CREATE TABLE product (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(200) NOT NULL,
    description VARCHAR2(2000),
    price NUMBER(10,2) NOT NULL,
    discounted_price NUMBER(10,2),
    quantity NUMBER NOT NULL,
    image_url VARCHAR2(255),
    category VARCHAR2(100) NOT NULL,
    seller_id NUMBER NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_product_seller FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE SEQUENCE product_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER product_trg
BEFORE INSERT ON product
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT product_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- =========================
-- ADDRESSES
-- =========================

CREATE TABLE addresses (
    id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    street VARCHAR2(200) NOT NULL,
    city VARCHAR2(100) NOT NULL,
    state VARCHAR2(100) NOT NULL,
    zip_code VARCHAR2(20) NOT NULL,
    country VARCHAR2(100) NOT NULL,
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE SEQUENCE address_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER address_trg
BEFORE INSERT ON addresses
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT address_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
END;
/

-- =========================
-- CARTS
-- =========================

CREATE TABLE carts (
    id NUMBER PRIMARY KEY,
    user_id NUMBER UNIQUE NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE SEQUENCE cart_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER carts_trg
BEFORE INSERT ON carts
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT cart_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- =========================
-- CART ITEM
-- =========================

CREATE TABLE cart_item (
    id NUMBER PRIMARY KEY,
    cart_id NUMBER NOT NULL,
    product_id NUMBER NOT NULL,
    quantity NUMBER NOT NULL,
    CONSTRAINT fk_cartitem_cart FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    CONSTRAINT fk_cartitem_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE SEQUENCE cart_item_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER cart_item_trg
BEFORE INSERT ON cart_item
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT cart_item_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
END;
/

-- =========================
-- ORDERS
-- =========================

CREATE TABLE orders (
    id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    total_amount NUMBER(10,2) NOT NULL,
    shipping_address_id NUMBER NOT NULL,
    billing_address_id NUMBER NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_shipaddr FOREIGN KEY (shipping_address_id) REFERENCES addresses(id),
    CONSTRAINT fk_order_billaddr FOREIGN KEY (billing_address_id) REFERENCES addresses(id)
);

CREATE SEQUENCE orders_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER orders_trg
BEFORE INSERT ON orders
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT orders_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
  :NEW.updated_at := SYSTIMESTAMP;
END;
/

-- =========================
-- ORDER ITEM
-- =========================

CREATE TABLE order_item (
    id NUMBER PRIMARY KEY,
    order_id NUMBER NOT NULL,
    product_id NUMBER NOT NULL,
    quantity NUMBER NOT NULL,
    price NUMBER(10,2) NOT NULL,
    status VARCHAR2(50) NOT NULL,
    CONSTRAINT fk_orderitem_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_orderitem_product FOREIGN KEY (product_id) REFERENCES product(id)
);

CREATE SEQUENCE order_item_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER order_item_trg
BEFORE INSERT ON order_item
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT order_item_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
END;
/

-- =========================
-- PAYMENTS
-- =========================

CREATE TABLE payments (
    id NUMBER PRIMARY KEY,
    order_id NUMBER UNIQUE NOT NULL,
    amount NUMBER(10,2) NOT NULL,
    payment_method VARCHAR2(100) NOT NULL,
    status VARCHAR2(50) NOT NULL,
    transaction_id VARCHAR2(100),
    created_at TIMESTAMP,
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE SEQUENCE payment_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER payments_trg
BEFORE INSERT ON payments
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT payment_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
END;
/

-- =========================
-- REVIEW
-- =========================

CREATE TABLE review (
    id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    product_id NUMBER NOT NULL,
    rating NUMBER NOT NULL,
    review_comment VARCHAR2(2000),
    created_at TIMESTAMP,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE SEQUENCE reviews_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER review_trg
BEFORE INSERT ON review
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT reviews_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
END;
/

-- =========================
-- FAVORITES
-- =========================

CREATE TABLE favorites (
    id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    product_id NUMBER NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_fav_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_fav_product FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE
);

CREATE SEQUENCE favorite_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER favorites_trg
BEFORE INSERT ON favorites
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT favorite_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
END;
/

-- =========================
-- NOTIFICATION
-- =========================

CREATE TABLE notification (
    id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    message VARCHAR2(1000) NOT NULL,
    is_read NUMBER(1) DEFAULT 0 NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE SEQUENCE notification_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER notification_trg
BEFORE INSERT ON notification
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT notification_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
END;
/

-- =========================
-- PASSWORD RECOVERY TOKENS
-- =========================

CREATE TABLE password_recovery_tokens (
    id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    token VARCHAR2(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    CONSTRAINT fk_recovery_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE SEQUENCE password_recovery_seq START WITH 1 INCREMENT BY 1;

CREATE OR REPLACE TRIGGER password_recovery_trg
BEFORE INSERT ON password_recovery_tokens
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT password_recovery_seq.NEXTVAL INTO :NEW.id FROM DUAL;
  END IF;
  :NEW.created_at := SYSTIMESTAMP;
END;
/