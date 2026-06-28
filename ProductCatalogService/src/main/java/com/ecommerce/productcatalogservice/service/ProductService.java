package com.ecommerce.productcatalogservice.service;

import com.ecommerce.productcatalogservice.dto.ProductRequest;
import com.ecommerce.productcatalogservice.dto.ProductResponse;
import com.ecommerce.productcatalogservice.dto.StockUpdateRequest;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);

    List<ProductResponse> getProducts(String category, String search, boolean includeInactive);

    ProductResponse getProductById(Long productId);

    List<String> getCategories();

    ProductResponse updateProduct(Long productId, ProductRequest request);

    ProductResponse updateStock(Long productId, StockUpdateRequest request);

    void deactivateProduct(Long productId);
}
