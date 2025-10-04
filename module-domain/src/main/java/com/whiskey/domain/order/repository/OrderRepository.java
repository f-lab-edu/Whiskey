package com.whiskey.domain.order.repository;

import com.whiskey.domain.order.Order;
import org.springframework.data.repository.CrudRepository;

public interface OrderRepository extends CrudRepository<Order, Long> {

}
