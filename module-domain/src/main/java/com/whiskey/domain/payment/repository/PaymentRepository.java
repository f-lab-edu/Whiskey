package com.whiskey.domain.payment.repository;

import com.whiskey.domain.order.Order;
import com.whiskey.domain.payment.Payment;
import com.whiskey.domain.payment.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByOrder(Order order);

    Optional<Payment> findByPaymentOrderId(String s);

    List<Payment> findByOrderAndPaymentStatus(Order order, PaymentStatus paymentStatus);
}
