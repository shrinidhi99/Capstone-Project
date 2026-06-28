package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order.placed", groupId = "order-service-group")
    public void handleOrderPlaced(String message) {
        log.info("[Kafka] Received message on topic 'order.placed'");
        try {
            OrderPlacedEvent event = objectMapper.readValue(message, OrderPlacedEvent.class);
            log.info("[Kafka] Deserialized OrderPlacedEvent — userId: {}, items: {}, total: {}",
                    event.getUserId(), event.getItems().size(), event.getTotalAmount());
            orderService.createOrder(event);
        } catch (JsonProcessingException e) {
            log.error("[Kafka] Failed to deserialize order.placed event: {}", e.getMessage());
        }
    }
}
