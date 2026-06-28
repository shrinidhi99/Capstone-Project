package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderItemResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.UpdateStatusRequest;
import com.ecommerce.orderservice.event.OrderConfirmedEvent;
import com.ecommerce.orderservice.event.OrderPlacedEvent;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_CONFIRMED_TOPIC = "order.confirmed";

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                            KafkaTemplate<String, String> kafkaTemplate,
                            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(OrderPlacedEvent event) {
        log.info("createOrder called — userId: {}, items: {}, total: {}",
                event.getUserId(), event.getItems().size(), event.getTotalAmount());

        Order order = Order.builder()
                .userId(event.getUserId())
                .deliveryAddress(event.getDeliveryAddress())
                .totalAmount(event.getTotalAmount())
                .status(OrderStatus.PENDING)
                .build();

        List<OrderItem> items = event.getItems().stream()
                .map(i -> OrderItem.builder()
                        .order(order)
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .price(i.getPrice())
                        .quantity(i.getQuantity())
                        .subtotal(i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();

        order.setItems(new ArrayList<>(items));

        Order savedOrder = orderRepository.save(order);
        log.info("[MySQL] Order created — id: {}, userId: {}, status: {}",
                savedOrder.getId(), savedOrder.getUserId(), savedOrder.getStatus());

        publishOrderConfirmedEvent(savedOrder);

        return mapToResponse(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        log.info("getOrderById called — orderId: {}", orderId);
        Order order = findOrder(orderId);
        log.info("[MySQL] Order fetched — id: {}, status: {}", order.getId(), order.getStatus());
        return mapToResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUserId(String userId) {
        log.info("getOrdersByUserId called — userId: {}", userId);
        List<Order> orders = orderRepository.findByUserIdOrderByPlacedAtDesc(userId);
        log.info("[MySQL] Fetched {} order(s) for userId: {}", orders.size(), userId);
        return orders.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateStatusRequest request) {
        log.info("updateOrderStatus called — orderId: {}, newStatus: {}", orderId, request.getStatus());

        Order order = findOrder(orderId);

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid status value provided: {}", request.getStatus());
            throw new IllegalArgumentException(
                    "Invalid status: " + request.getStatus() + ". Valid values: PENDING, CONFIRMED, SHIPPED, DELIVERED");
        }

        log.info("Updating order {} status: {} → {}", orderId, order.getStatus(), newStatus);
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        log.info("[MySQL] Order status updated — id: {}, status: {}", orderId, savedOrder.getStatus());

        return mapToResponse(savedOrder);
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> {
            log.warn("[MySQL] Order not found — id: {}", orderId);
            return new OrderNotFoundException(orderId);
        });
    }

    private void publishOrderConfirmedEvent(Order order) {
        OrderConfirmedEvent event = OrderConfirmedEvent.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .confirmedAt(LocalDateTime.now())
                .build();

        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(ORDER_CONFIRMED_TOPIC, order.getUserId(), payload);
            log.info("[Kafka] Published order.confirmed event — orderId: {}, userId: {}",
                    order.getId(), order.getUserId());
        } catch (JsonProcessingException e) {
            log.error("[Kafka] Failed to publish order.confirmed event for orderId: {}", order.getId());
        }
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(i -> OrderItemResponse.builder()
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .price(i.getPrice())
                        .quantity(i.getQuantity())
                        .subtotal(i.getSubtotal())
                        .build())
                .toList();

        return OrderResponse.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .deliveryAddress(order.getDeliveryAddress())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus().name())
                .items(itemResponses)
                .placedAt(order.getPlacedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
