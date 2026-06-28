package com.ecommerce.productcatalogservice.service;

import com.ecommerce.productcatalogservice.dto.ProductRequest;
import com.ecommerce.productcatalogservice.dto.ProductResponse;
import com.ecommerce.productcatalogservice.dto.StockUpdateRequest;

import java.util.List;

public interface ProductService {

    /** Create and persist a new product. */
    ProductResponse createProduct(ProductRequest request);

    /** List products filtered by category, search term, or active status. */
    List<ProductResponse> getProducts(String category, String search, boolean includeInactive);

    /** Fetch a single active product by ID. */
    ProductResponse getProductById(Long productId);

    /** List all distinct categories that have at least one active product. */
    List<String> getCategories();

    /** Replace all fields of an existing product. */
    ProductResponse updateProduct(Long productId, ProductRequest request);

    /** Update stock quantity only, leaving all other fields unchanged. */
    ProductResponse updateStock(Long productId, StockUpdateRequest request);

    /** Soft-delete a product by setting active to false. */
    void deactivateProduct(Long productId);
}
