package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.UpdateStatusRequest;
import com.ecommerce.orderservice.event.OrderPlacedEvent;

import java.util.List;

public interface OrderService {

    /** Persist order from Kafka event and publish order.confirmed. */
    OrderResponse createOrder(OrderPlacedEvent event);

    /** Fetch a single order by ID. */
    OrderResponse getOrderById(Long orderId);

    /** Fetch all orders for a user, newest first. */
    List<OrderResponse> getOrdersByUserId(String userId);

    /** Change order status and persist. */
    OrderResponse updateOrderStatus(Long orderId, UpdateStatusRequest request);
}
