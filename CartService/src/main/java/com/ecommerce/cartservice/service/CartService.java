package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddItemRequest;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.CheckoutRequest;
import com.ecommerce.cartservice.dto.CheckoutResponse;
import com.ecommerce.cartservice.dto.UpdateQuantityRequest;

public interface CartService {

    /** Adds a product to the cart, or increments quantity if already present. */
    CartResponse addItem(String userId, AddItemRequest request);

    /** Returns cart from Redis cache, falling back to MongoDB on miss. */
    CartResponse getCart(String userId);

    /** Sets the quantity of a specific item in the cart. */
    CartResponse updateItemQuantity(String userId, Long productId, UpdateQuantityRequest request);

    /** Removes a specific product from the cart. */
    CartResponse removeItem(String userId, Long productId);

    /** Clears all items from the cart and resets the total to zero. */
    void clearCart(String userId);

    /** Publishes an order.placed Kafka event and clears the cart. */
    CheckoutResponse checkout(String userId, CheckoutRequest request);
}
