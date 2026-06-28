package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.UpdateStatusRequest;
import com.ecommerce.orderservice.event.OrderPlacedEvent;
import com.ecommerce.orderservice.exception.OrderNotFoundException;
import com.ecommerce.orderservice.model.Order;
import com.ecommerce.orderservice.model.OrderItem;
import com.ecommerce.orderservice.model.OrderStatus;
import com.ecommerce.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ObjectMapper objectMapper;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        orderService = new OrderServiceImpl(orderRepository, kafkaTemplate, objectMapper);
    }

    @Test
    void createOrder_success() {
        OrderPlacedEvent.OrderItem eventItem = new OrderPlacedEvent.OrderItem(1L, "iPhone 15", BigDecimal.valueOf(79999), 1);
        OrderPlacedEvent event = new OrderPlacedEvent(
                "user-1",
                List.of(eventItem),
                BigDecimal.valueOf(79999),
                "123 Main St, Bangalore",
                LocalDateTime.now()
        );

        Order savedOrder = Order.builder()
                .id(1L)
                .userId("user-1")
                .deliveryAddress("123 Main St, Bangalore")
                .totalAmount(BigDecimal.valueOf(79999))
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .placedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.createOrder(event);

        assertThat(response.getOrderId()).isEqualTo(1L);
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        verify(orderRepository).save(any(Order.class));
        verify(kafkaTemplate).send(eq("order.confirmed"), eq("user-1"), any(String.class));
    }

    @Test
    void getOrderById_success() {
        Order order = Order.builder()
                .id(10L)
                .userId("user-1")
                .deliveryAddress("456 Park Ave")
                .totalAmount(BigDecimal.valueOf(29999))
                .status(OrderStatus.CONFIRMED)
                .items(new ArrayList<>())
                .placedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(10L);

        assertThat(response.getOrderId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    void getOrderById_notFound_throwsOrderNotFoundException() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(99L));
    }

    @Test
    void getOrdersByUserId_returnsList() {
        Order order = Order.builder()
                .id(1L)
                .userId("user-1")
                .deliveryAddress("123 Main St")
                .totalAmount(BigDecimal.valueOf(5000))
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .placedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepository.findByUserIdOrderByPlacedAtDesc("user-1")).thenReturn(List.of(order));

        List<OrderResponse> responses = orderService.getOrdersByUserId("user-1");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUserId()).isEqualTo("user-1");
    }

    @Test
    void getOrdersByUserId_noOrders_returnsEmptyList() {
        when(orderRepository.findByUserIdOrderByPlacedAtDesc("user-99")).thenReturn(List.of());

        List<OrderResponse> responses = orderService.getOrdersByUserId("user-99");

        assertThat(responses).isEmpty();
    }

    @Test
    void updateOrderStatus_success() {
        Order order = Order.builder()
                .id(1L)
                .userId("user-1")
                .deliveryAddress("123 Main St")
                .totalAmount(BigDecimal.valueOf(79999))
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .placedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Order savedOrder = Order.builder()
                .id(1L)
                .userId("user-1")
                .deliveryAddress("123 Main St")
                .totalAmount(BigDecimal.valueOf(79999))
                .status(OrderStatus.CONFIRMED)
                .items(new ArrayList<>())
                .placedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("CONFIRMED");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.updateOrderStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo("CONFIRMED");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_invalidStatus_throwsIllegalArgumentException() {
        Order order = Order.builder()
                .id(1L)
                .userId("user-1")
                .deliveryAddress("123 Main St")
                .totalAmount(BigDecimal.valueOf(79999))
                .status(OrderStatus.PENDING)
                .items(new ArrayList<>())
                .placedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("INVALID_STATUS");

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrderStatus(1L, request));
    }

    @Test
    void updateOrderStatus_orderNotFound_throwsOrderNotFoundException() {
        UpdateStatusRequest request = new UpdateStatusRequest();
        request.setStatus("CONFIRMED");

        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.updateOrderStatus(99L, request));
    }
}
