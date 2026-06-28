package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.UpdateStatusRequest;
import com.ecommerce.orderservice.event.OrderPlacedEvent;

import java.util.List;

public interface OrderService {

    /** Called from the Kafka consumer, not an HTTP endpoint — persists order and fires order.confirmed. */
    OrderResponse createOrder(OrderPlacedEvent event);

    OrderResponse getOrderById(Long orderId);

    /** Sorted newest-first. */
    List<OrderResponse> getOrdersByUserId(String userId);

    OrderResponse updateOrderStatus(Long orderId, UpdateStatusRequest request);
}
