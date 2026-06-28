package com.ecommerce.productcatalogservice.service;

import com.ecommerce.productcatalogservice.dto.ProductRequest;
import com.ecommerce.productcatalogservice.dto.ProductResponse;
import com.ecommerce.productcatalogservice.dto.StockUpdateRequest;
import com.ecommerce.productcatalogservice.exception.ProductNotFoundException;
import com.ecommerce.productcatalogservice.model.Product;
import com.ecommerce.productcatalogservice.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("createProduct called — name: {}, category: {}, brand: {}, price: {}, quantity: {}",
                request.getName(), request.getCategory(), request.getBrand(), request.getPrice(), request.getQuantity());

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .brand(request.getBrand())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .imageUrl(request.getImageUrl())
                .active(true)
                .build();

        Product saved = productRepository.save(product);
        log.info("[MySQL] Product created — id: {}, name: {}", saved.getId(), saved.getName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(String category, String search, boolean includeInactive) {
        log.info("getProducts called — category: {}, search: {}, includeInactive: {}", category, search, includeInactive);

        List<Product> products;

        if (search != null && !search.isBlank()) {
            log.info("[MySQL] Querying products by search term: '{}'", search);
            products = productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                    search, search, search);
        } else if (category != null && !category.isBlank()) {
            log.info("[MySQL] Querying products by category: '{}'", category);
            products = productRepository.findByCategoryIgnoreCaseAndActiveTrueOrderByCreatedAtDesc(category);
        } else if (includeInactive) {
            log.info("[MySQL] Querying all products including inactive");
            products = productRepository.findAll();
        } else {
            log.info("[MySQL] Querying all active products");
            products = productRepository.findByActiveTrueOrderByCreatedAtDesc();
        }

        List<ProductResponse> result = products.stream()
                .filter(product -> includeInactive || product.isActive())
                .map(this::mapToResponse)
                .toList();

        log.info("getProducts returning {} product(s)", result.size());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        log.info("getProductById called — productId: {}", productId);
        ProductResponse response = mapToResponse(findProduct(productId));
        log.info("[MySQL] Product fetched — id: {}, name: {}", productId, response.getName());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategories() {
        log.info("getCategories called");
        List<String> categories = productRepository.findActiveCategories();
        log.info("[MySQL] Found {} active category(s)", categories.size());
        return categories;
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request) {
        log.info("updateProduct called — productId: {}, name: {}, price: {}, quantity: {}",
                productId, request.getName(), request.getPrice(), request.getQuantity());

        Product product = findProduct(productId);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());
        product.setBrand(request.getBrand());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setImageUrl(request.getImageUrl());

        Product saved = productRepository.save(product);
        log.info("[MySQL] Product updated — id: {}, name: {}", saved.getId(), saved.getName());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long productId, StockUpdateRequest request) {
        log.info("updateStock called — productId: {}, newQuantity: {}", productId, request.getQuantity());

        Product product = findProduct(productId);
        int previousQuantity = product.getQuantity();
        product.setQuantity(request.getQuantity());

        Product saved = productRepository.save(product);
        log.info("[MySQL] Stock updated for product {} — quantity: {} → {}", productId, previousQuantity, saved.getQuantity());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deactivateProduct(Long productId) {
        log.info("deactivateProduct called — productId: {}", productId);

        Product product = findProduct(productId);
        product.setActive(false);
        productRepository.save(product);
        log.info("[MySQL] Product deactivated — id: {}, name: {}", productId, product.getName());
    }

    private Product findProduct(Long productId) {
        log.info("[MySQL] Fetching product — id: {}", productId);
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("[MySQL] Product not found — id: {}", productId);
                    return new ProductNotFoundException(productId);
                });
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .imageUrl(product.getImageUrl())
                .active(product.isActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
