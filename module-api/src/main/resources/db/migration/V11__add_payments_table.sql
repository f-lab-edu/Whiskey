CREATE TABLE payments (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    payment_order_id varchar(255) NOT NULL,
    payment_key varchar(255) NOT NULL,
    member_id bigint NOT NULL,
    order_id bigint,
    amount bigint NOT NULL,
    payment_status enum('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED'),
    description varchar(500),
    request_date datetime,
    approved_date datetime,
    created_at datetime,
    updated_at datetime
);

-- 유니크 키
CREATE UNIQUE INDEX uk_payment_order_id ON payments(payment_order_id);

-- 인덱스
CREATE INDEX idx_member_id ON payments(member_id);
CREATE INDEX idx_order_id ON payments(order_id);
CREATE INDEX idx_payment_status ON payments(payment_status);

-- 외래 키
ALTER TABLE payments ADD CONSTRAINT fk_payments_member FOREIGN KEY (member_id) REFERENCES member(id);
ALTER TABLE payments ADD CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id);