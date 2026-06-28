package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddItemRequest;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.CheckoutRequest;
import com.ecommerce.cartservice.dto.CheckoutResponse;
import com.ecommerce.cartservice.dto.UpdateQuantityRequest;

public interface CartService {

    /** Upserts — increments quantity if the product is already in the cart. */
    CartResponse addItem(String userId, AddItemRequest request);

    /** Redis-first; falls back to MongoDB on cache miss. */
    CartResponse getCart(String userId);

    CartResponse updateItemQuantity(String userId, Long productId, UpdateQuantityRequest request);

    CartResponse removeItem(String userId, Long productId);

    void clearCart(String userId);

    /** Fires order.placed Kafka event then wipes the cart. */
    CheckoutResponse checkout(String userId, CheckoutRequest request);
}
