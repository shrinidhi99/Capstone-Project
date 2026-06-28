package com.ecommerce.cartservice.exception;

public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(String userId, Long productId) {
        super("Product " + productId + " not found in cart for user: " + userId);
    }
}
