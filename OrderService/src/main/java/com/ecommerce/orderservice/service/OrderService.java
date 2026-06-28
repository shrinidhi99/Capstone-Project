package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.UpdateStatusRequest;
import com.ecommerce.orderservice.event.OrderPlacedEvent;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderPlacedEvent event);
    OrderResponse getOrderById(Long orderId);
    List<OrderResponse> getOrdersByUserId(String userId);
    OrderResponse updateOrderStatus(Long orderId, UpdateStatusRequest request);
}
