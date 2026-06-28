package com.ecommerce.productcatalogservice.service;

import com.ecommerce.productcatalogservice.dto.ProductRequest;
import com.ecommerce.productcatalogservice.dto.ProductResponse;
import com.ecommerce.productcatalogservice.dto.StockUpdateRequest;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    /** category and search are both optional — pass null to skip either filter. */
    List<ProductResponse> getProducts(String category, String search, boolean includeInactive);

    /** Only active products are returned; throws ProductNotFoundException for deactivated ones. */
    ProductResponse getProductById(Long productId);

    /** Only categories with at least one active product show up here. */
    List<String> getCategories();

    /** Full replacement, not a patch — all fields are overwritten. */
    ProductResponse updateProduct(Long productId, ProductRequest request);

    ProductResponse updateStock(Long productId, StockUpdateRequest request);

    /** Soft delete — sets active=false, record stays in DB. */
    void deactivateProduct(Long productId);
}
