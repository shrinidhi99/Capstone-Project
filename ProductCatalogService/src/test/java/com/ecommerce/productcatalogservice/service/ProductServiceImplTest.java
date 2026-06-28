package com.ecommerce.productcatalogservice.service;

import com.ecommerce.productcatalogservice.dto.ProductRequest;
import com.ecommerce.productcatalogservice.dto.ProductResponse;
import com.ecommerce.productcatalogservice.dto.StockUpdateRequest;
import com.ecommerce.productcatalogservice.exception.ProductNotFoundException;
import com.ecommerce.productcatalogservice.model.Product;
import com.ecommerce.productcatalogservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product sampleProduct;
    private ProductRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.builder()
                .id(1L)
                .name("iPhone 15")
                .description("Apple smartphone")
                .category("Mobiles")
                .brand("Apple")
                .price(new BigDecimal("79999.00"))
                .quantity(25)
                .imageUrl("https://example.com/iphone.jpg")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        sampleRequest = new ProductRequest();
        sampleRequest.setName("iPhone 15");
        sampleRequest.setDescription("Apple smartphone");
        sampleRequest.setCategory("Mobiles");
        sampleRequest.setBrand("Apple");
        sampleRequest.setPrice(new BigDecimal("79999.00"));
        sampleRequest.setQuantity(25);
        sampleRequest.setImageUrl("https://example.com/iphone.jpg");
    }

    @Test
    void createProduct_success() {
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        ProductResponse response = productService.createProduct(sampleRequest);

        assertThat(response.getName()).isEqualTo("iPhone 15");
        assertThat(response.getCategory()).isEqualTo("Mobiles");
        assertThat(response.getPrice()).isEqualByComparingTo("79999.00");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void getProducts_noFilters_returnsAllActive() {
        when(productRepository.findByActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(sampleProduct));

        List<ProductResponse> result = productService.getProducts(null, null, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("iPhone 15");
        verify(productRepository).findByActiveTrueOrderByCreatedAtDesc();
    }

    @Test
    void getProducts_categoryFilter_callsCategoryRepo() {
        when(productRepository.findByCategoryIgnoreCaseAndActiveTrueOrderByCreatedAtDesc("Mobiles"))
                .thenReturn(List.of(sampleProduct));

        List<ProductResponse> result = productService.getProducts("Mobiles", null, false);

        assertThat(result).hasSize(1);
        verify(productRepository).findByCategoryIgnoreCaseAndActiveTrueOrderByCreatedAtDesc("Mobiles");
    }

    @Test
    void getProducts_searchTerm_callsSearchRepo() {
        when(productRepository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                "iphone", "iphone", "iphone")).thenReturn(List.of(sampleProduct));

        List<ProductResponse> result = productService.getProducts(null, "iphone", false);

        assertThat(result).hasSize(1);
        verify(productRepository).findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase(
                "iphone", "iphone", "iphone");
    }

    @Test
    void getProductById_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("iPhone 15");
    }

    @Test
    void getProductById_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProductById(99L));
    }

    @Test
    void getCategories_returnsListFromRepo() {
        when(productRepository.findActiveCategories()).thenReturn(List.of("Mobiles", "Laptops", "Audio"));

        List<String> categories = productService.getCategories();

        assertThat(categories).containsExactly("Mobiles", "Laptops", "Audio");
        verify(productRepository).findActiveCategories();
    }

    @Test
    void updateProduct_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        sampleRequest.setPrice(new BigDecimal("74999.00"));
        sampleRequest.setQuantity(20);

        ProductResponse response = productService.updateProduct(1L, sampleRequest);

        verify(productRepository).save(any(Product.class));
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void updateProduct_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(99L, sampleRequest));
    }

    @Test
    void updateStock_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            return p;
        });

        StockUpdateRequest stockRequest = new StockUpdateRequest();
        stockRequest.setQuantity(50);

        ProductResponse response = productService.updateStock(1L, stockRequest);

        assertThat(response.getQuantity()).isEqualTo(50);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateStock_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        StockUpdateRequest stockRequest = new StockUpdateRequest();
        stockRequest.setQuantity(10);

        assertThrows(ProductNotFoundException.class, () -> productService.updateStock(99L, stockRequest));
    }

    @Test
    void deactivateProduct_success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        productService.deactivateProduct(1L);

        assertThat(sampleProduct.isActive()).isFalse();
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void deactivateProduct_notFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.deactivateProduct(99L));
    }
}
