CREATE TABLE orders (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    member_id bigint NOT NULL,
    total_price decimal(19, 2),
    order_status enum('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED'),
    payment_id varchar(100),
    expire_at datetime NOT NULL,
    confirmed_at datetime,
    cancelled_at datetime,
    create_at datetime,
    update_at datetime
);

CREATE TABLE stocks (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    whiskey_id bigint NOT NULL,
    quantity int NOT NULL,
    available_quantity int NOT NULL,
    price decimal(19, 2) NOT NULL,
    stock_status enum('SOLD_OUT', 'IN_STOCK') NOT NULL,
    version bigint,
    create_at datetime,
    update_at datetime,
    FOREIGN KEY (whiskey_id) REFERENCES whiskey(id)
);

CREATE TABLE stock_reservation (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    order_id bigint NOT NULL,
    stock_id bigint NOT NULL,
    reserved_quantity int NOT NULL,
    reservation_status enum('RESERVED', 'CONFIRMED', 'CANCELLED', 'EXPIRED') NOT NULL,
    expire_at DATETIME,
    confirmed_at DATETIME,
    cancelled_at DATETIME,
    create_at DATETIME NOT NULL,
    update_at DATETIME NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
);