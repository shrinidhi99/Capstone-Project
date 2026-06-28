package com.ecommerce.productcatalogservice.repository;

import com.ecommerce.productcatalogservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    List<Product> findByCategoryIgnoreCaseAndActiveTrueOrderByCreatedAtDesc(String category);

    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrCategoryContainingIgnoreCase(
            String name,
            String description,
            String category
    );

    @Query("select distinct p.category from Product p where p.active = true order by p.category")
    List<String> findActiveCategories();
}
