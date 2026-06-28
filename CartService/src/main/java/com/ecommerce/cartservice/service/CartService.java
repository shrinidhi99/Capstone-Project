package com.ecommerce.cartservice.service;

import com.ecommerce.cartservice.dto.AddItemRequest;
import com.ecommerce.cartservice.dto.CartResponse;
import com.ecommerce.cartservice.dto.CheckoutRequest;
import com.ecommerce.cartservice.dto.CheckoutResponse;
import com.ecommerce.cartservice.dto.UpdateQuantityRequest;

public interface CartService {
    CartResponse addItem(String userId, AddItemRequest request);
    CartResponse getCart(String userId);
    CartResponse updateItemQuantity(String userId, Long productId, UpdateQuantityRequest request);
    CartResponse removeItem(String userId, Long productId);
    void clearCart(String userId);
    CheckoutResponse checkout(String userId, CheckoutRequest request);
}
