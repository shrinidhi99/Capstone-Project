package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddItemRequest;
import com.ecommerce.cartservice.dto.CartItemResponse;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.CheckoutRequest;
import com.ecommerce.cartservice.dto.CheckoutResponse;
import com.ecommerce.cartservice.dto.UpdateQuantityRequest;
import com.ecommerce.cartservice.event.OrderPlacedEvent;
import com.ecommerce.cartservice.exception.CartItemNotFoundException;
import com.ecommerce.cartservice.exception.CartNotFoundException;
import com.ecommerce.cartservice.model.Cart;
import com.ecommerce.cartservice.model.CartItem;
import com.ecommerce.cartservice.repository.CartRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class CartServiceImpl implements CartService {
    private static final String CART_CACHE_PREFIX = "cart:";
    private static final String ORDER_PLACED_TOPIC = "order.placed";
    private static final Duration CART_CACHE_TTL = Duration.ofMinutes(30);

    private final CartRepository cartRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public CartServiceImpl(CartRepository cartRepository,
                           StringRedisTemplate redisTemplate,
                           KafkaTemplate<String, String> kafkaTemplate,
                           ObjectMapper objectMapper) {
        this.cartRepository = cartRepository;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public CartResponse addItem(String userId, AddItemRequest request) {
        log.info("addItem called — userId: {}, productId: {}, productName: {}, price: {}, quantity: {}",
                userId, request.getProductId(), request.getProductName(), request.getPrice(), request.getQuantity());

        Cart cart = cartRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("[MongoDB] No existing cart for user {} — initialising new cart", userId);
                    return Cart.builder().userId(userId).items(new ArrayList<>()).build();
                });

        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.getProductId()))
                .findFirst()
                .ifPresentOrElse(
                        existingItem -> {
                            int updatedQty = existingItem.getQuantity() + request.getQuantity();
                            log.info("Product {} already in cart — incrementing quantity from {} to {}",
                                    request.getProductId(), existingItem.getQuantity(), updatedQty);
                            existingItem.setQuantity(updatedQty);
                        },
                        () -> {
                            log.info("Product {} not in cart — adding as new item", request.getProductId());
                            cart.getItems().add(CartItem.builder()
                                    .productId(request.getProductId())
                                    .productName(request.getProductName())
                                    .price(request.getPrice())
                                    .quantity(request.getQuantity())
                                    .build());
                        }
                );

        cart.setTotalAmount(calculateTotal(cart.getItems()));
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        log.info("[MongoDB] Cart saved for user {} — {} item(s), total: {}", userId, savedCart.getItems().size(), savedCart.getTotalAmount());

        evictCache(userId);
        return mapToResponse(savedCart);
    }

    @Override
    public CartResponse getCart(String userId) {
        log.info("getCart called — userId: {}", userId);

        String cacheKey = CART_CACHE_PREFIX + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            log.info("[Redis] Cache hit for key {} — returning cached cart", cacheKey);
            return mapToResponse(deserializeCart(cached));
        }

        log.info("[Redis] Cache miss for key {} — fetching from MongoDB", cacheKey);
        Cart cart = findCartByUserId(userId);
        redisTemplate.opsForValue().set(cacheKey, serialize(cart), CART_CACHE_TTL);
        log.info("[Redis] Cart for user {} cached with TTL {} minutes", userId, CART_CACHE_TTL.toMinutes());

        return mapToResponse(cart);
    }

    @Override
    public CartResponse updateItemQuantity(String userId, Long productId, UpdateQuantityRequest request) {
        log.info("updateItemQuantity called — userId: {}, productId: {}, newQuantity: {}",
                userId, productId, request.getQuantity());

        Cart cart = findCartByUserId(userId);
        CartItem item = findItemInCart(cart, productId);
        log.info("Product {} found in cart — updating quantity from {} to {}", productId, item.getQuantity(), request.getQuantity());

        item.setQuantity(request.getQuantity());
        cart.setTotalAmount(calculateTotal(cart.getItems()));
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        log.info("[MongoDB] Cart updated for user {} — new total: {}", userId, savedCart.getTotalAmount());

        evictCache(userId);
        return mapToResponse(savedCart);
    }

    @Override
    public CartResponse removeItem(String userId, Long productId) {
        log.info("removeItem called — userId: {}, productId: {}", userId, productId);

        Cart cart = findCartByUserId(userId);
        boolean removed = cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        if (!removed) {
            log.warn("Product {} not found in cart for user {} — throwing CartItemNotFoundException", productId, userId);
            throw new CartItemNotFoundException(userId, productId);
        }

        cart.setTotalAmount(calculateTotal(cart.getItems()));
        cart.setUpdatedAt(LocalDateTime.now());
        Cart savedCart = cartRepository.save(cart);
        log.info("[MongoDB] Product {} removed from cart for user {} — {} item(s) remaining, total: {}",
                productId, userId, savedCart.getItems().size(), savedCart.getTotalAmount());

        evictCache(userId);
        return mapToResponse(savedCart);
    }

    @Override
    public void clearCart(String userId) {
        log.info("clearCart called — userId: {}", userId);

        Cart cart = findCartByUserId(userId);
        int itemCount = cart.getItems().size();
        cart.getItems().clear();
        cart.setTotalAmount(BigDecimal.ZERO);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        log.info("[MongoDB] Cart cleared for user {} — removed {} item(s)", userId, itemCount);

        evictCache(userId);
    }

    @Override
    public CheckoutResponse checkout(String userId, CheckoutRequest request) {
        log.info("checkout called — userId: {}, deliveryAddress: {}", userId, request.getDeliveryAddress());

        Cart cart = findCartByUserId(userId);
        if (cart.getItems().isEmpty()) {
            log.warn("Checkout attempted with empty cart for user {}", userId);
            throw new IllegalStateException("Cannot checkout with an empty cart");
        }

        LocalDateTime now = LocalDateTime.now();
        OrderPlacedEvent event = OrderPlacedEvent.builder()
                .userId(userId)
                .items(cart.getItems().stream()
                        .map(item -> new OrderPlacedEvent.OrderItem(
                                item.getProductId(), item.getProductName(), item.getPrice(), item.getQuantity()))
                        .toList())
                .totalAmount(cart.getTotalAmount())
                .deliveryAddress(request.getDeliveryAddress())
                .placedAt(now)
                .build();

        log.info("[Kafka] Publishing event to topic '{}' — userId: {}, {} item(s), total: {}",
                ORDER_PLACED_TOPIC, userId, event.getItems().size(), event.getTotalAmount());
        kafkaTemplate.send(ORDER_PLACED_TOPIC, userId, serialize(event));
        log.info("[Kafka] Event published successfully for user {}", userId);

        clearCart(userId);

        log.info("Checkout complete for user {} — order placed with total {}", userId, event.getTotalAmount());
        return CheckoutResponse.builder()
                .userId(userId)
                .deliveryAddress(request.getDeliveryAddress())
                .totalAmount(event.getTotalAmount())
                .itemCount(event.getItems().size())
                .status("ORDER_PLACED")
                .checkedOutAt(now)
                .build();
    }

    private Cart findCartByUserId(String userId) {
        log.info("[MongoDB] Fetching cart for user {}", userId);
        return cartRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[MongoDB] Cart not found for user {}", userId);
                    return new CartNotFoundException(userId);
                });
    }

    private CartItem findItemInCart(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("Product {} not found in cart for user {}", productId, cart.getUserId());
                    return new CartItemNotFoundException(cart.getUserId(), productId);
                });
    }

    private void evictCache(String userId) {
        redisTemplate.delete(CART_CACHE_PREFIX + userId);
        log.info("[Redis] Cache evicted for key {}{}", CART_CACHE_PREFIX, userId);
    }

    private BigDecimal calculateTotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartResponse mapToResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> CartItemResponse.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build())
                .toList();

        return CartResponse.builder()
                .userId(cart.getUserId())
                .items(itemResponses)
                .totalAmount(cart.getTotalAmount())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Serialization failed", e);
        }
    }

    private Cart deserializeCart(String json) {
        try {
            return objectMapper.readValue(json, Cart.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Deserialization failed", e);
        }
    }
}
