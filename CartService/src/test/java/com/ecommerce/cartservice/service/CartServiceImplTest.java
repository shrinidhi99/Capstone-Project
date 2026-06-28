package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddItemRequest;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.CheckoutRequest;
import com.ecommerce.cartservice.dto.CheckoutResponse;
import com.ecommerce.cartservice.dto.UpdateQuantityRequest;
import com.ecommerce.cartservice.exception.CartItemNotFoundException;
import com.ecommerce.cartservice.exception.CartNotFoundException;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;
import com.ecommerce.cartservice.repository.CartRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CartServiceImpl cartService;

    private static final String USER_ID = "user1";

    private AddItemRequest buildAddItemRequest(Long productId, String name, BigDecimal price, int qty) {
        AddItemRequest req = new AddItemRequest();
        req.setProductId(productId);
        req.setProductName(name);
        req.setPrice(price);
        req.setQuantity(qty);
        return req;
    }

    private Cart buildCart(String userId, List<CartItem> items) {
        BigDecimal total = items.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Cart.builder()
                .userId(userId)
                .items(new ArrayList<>(items))
                .totalAmount(total)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @BeforeEach
    void setUpRedis() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
    }

    // --- addItem ---

    @Test
    void addItem_newCart() {
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.empty());
        AddItemRequest req = buildAddItemRequest(1L, "Phone", new BigDecimal("999.00"), 1);
        Cart saved = buildCart(USER_ID, List.of(CartItem.builder()
                .productId(1L).productName("Phone").price(new BigDecimal("999.00")).quantity(1).build()));
        when(cartRepository.save(any())).thenReturn(saved);

        CartResponse response = cartService.addItem(USER_ID, req);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        assertThat(response.getItems()).hasSize(1);
        verify(cartRepository).save(any());
        verify(redisTemplate).delete("cart:" + USER_ID);
    }

    @Test
    void addItem_existingItemQuantityIncremented() {
        CartItem existing = CartItem.builder()
                .productId(1L).productName("Phone").price(new BigDecimal("999.00")).quantity(2).build();
        Cart cart = buildCart(USER_ID, new ArrayList<>(List.of(existing)));
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));

        AddItemRequest req = buildAddItemRequest(1L, "Phone", new BigDecimal("999.00"), 3);
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.addItem(USER_ID, req);

        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addItem_newItemAddedToExistingCart() {
        CartItem existing = CartItem.builder()
                .productId(1L).productName("Phone").price(new BigDecimal("999.00")).quantity(1).build();
        Cart cart = buildCart(USER_ID, new ArrayList<>(List.of(existing)));
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));

        AddItemRequest req = buildAddItemRequest(2L, "Laptop", new BigDecimal("1500.00"), 1);
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.addItem(USER_ID, req);

        assertThat(response.getItems()).hasSize(2);
    }

    // --- getCart ---

    @Test
    void getCart_cacheHit() throws Exception {
        Cart cart = buildCart(USER_ID, List.of(
                CartItem.builder().productId(1L).productName("Phone")
                        .price(new BigDecimal("999.00")).quantity(1).build()));
        String cached = objectMapper.writeValueAsString(cart);
        when(valueOperations.get("cart:" + USER_ID)).thenReturn(cached);

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        verify(cartRepository, never()).findById(any());
    }

    @Test
    void getCart_cacheMiss_fetchesFromMongoDB() {
        Cart cart = buildCart(USER_ID, List.of(
                CartItem.builder().productId(1L).productName("Phone")
                        .price(new BigDecimal("999.00")).quantity(1).build()));
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));

        CartResponse response = cartService.getCart(USER_ID);

        assertThat(response.getUserId()).isEqualTo(USER_ID);
        verify(cartRepository).findById(USER_ID);
        verify(valueOperations).set(eq("cart:" + USER_ID), anyString(), any());
    }

    // --- updateItemQuantity ---

    @Test
    void updateItemQuantity_success() {
        CartItem item = CartItem.builder()
                .productId(1L).productName("Phone").price(new BigDecimal("999.00")).quantity(1).build();
        Cart cart = buildCart(USER_ID, new ArrayList<>(List.of(item)));
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateQuantityRequest req = new UpdateQuantityRequest();
        req.setQuantity(5);

        CartResponse response = cartService.updateItemQuantity(USER_ID, 1L, req);

        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);
        verify(cartRepository).save(any());
    }

    @Test
    void updateItemQuantity_cartNotFound_throwsException() {
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.empty());
        UpdateQuantityRequest req = new UpdateQuantityRequest();
        req.setQuantity(3);

        assertThrows(CartNotFoundException.class, () -> cartService.updateItemQuantity(USER_ID, 1L, req));
    }

    @Test
    void updateItemQuantity_itemNotFound_throwsException() {
        Cart cart = buildCart(USER_ID, new ArrayList<>());
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));
        UpdateQuantityRequest req = new UpdateQuantityRequest();
        req.setQuantity(3);

        assertThrows(CartItemNotFoundException.class, () -> cartService.updateItemQuantity(USER_ID, 99L, req));
    }

    // --- removeItem ---

    @Test
    void removeItem_success() {
        CartItem item = CartItem.builder()
                .productId(1L).productName("Phone").price(new BigDecimal("999.00")).quantity(1).build();
        Cart cart = buildCart(USER_ID, new ArrayList<>(List.of(item)));
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.removeItem(USER_ID, 1L);

        assertThat(response.getItems()).isEmpty();
        verify(cartRepository).save(any());
    }

    @Test
    void removeItem_itemNotFound_throwsException() {
        Cart cart = buildCart(USER_ID, new ArrayList<>());
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));

        assertThrows(CartItemNotFoundException.class, () -> cartService.removeItem(USER_ID, 99L));
    }

    // --- clearCart ---

    @Test
    void clearCart_success() {
        CartItem item = CartItem.builder()
                .productId(1L).productName("Phone").price(new BigDecimal("999.00")).quantity(1).build();
        Cart cart = buildCart(USER_ID, new ArrayList<>(List.of(item)));
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        cartService.clearCart(USER_ID);

        verify(cartRepository).save(argThat(c -> c.getItems().isEmpty()
                && c.getTotalAmount().compareTo(BigDecimal.ZERO) == 0));
        verify(redisTemplate).delete("cart:" + USER_ID);
    }

    // --- checkout ---

    @Test
    void checkout_success() {
        CartItem item = CartItem.builder()
                .productId(1L).productName("Phone").price(new BigDecimal("999.00")).quantity(2).build();
        Cart cart = buildCart(USER_ID, new ArrayList<>(List.of(item)));
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CheckoutRequest req = new CheckoutRequest();
        req.setDeliveryAddress("123 Main St");

        CheckoutResponse response = cartService.checkout(USER_ID, req);

        assertThat(response.getStatus()).isEqualTo("ORDER_PLACED");
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("1998.00"));
        verify(kafkaTemplate).send(eq("order.placed"), eq(USER_ID), anyString());
    }

    @Test
    void checkout_emptyCart_throwsException() {
        Cart cart = buildCart(USER_ID, new ArrayList<>());
        when(cartRepository.findById(USER_ID)).thenReturn(Optional.of(cart));

        CheckoutRequest req = new CheckoutRequest();
        req.setDeliveryAddress("123 Main St");

        assertThrows(IllegalStateException.class, () -> cartService.checkout(USER_ID, req));
    }
}
