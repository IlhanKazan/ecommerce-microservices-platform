package com.ecommerce.productservice.category.repository;

import com.ecommerce.productservice.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentCategoryIsNullAndIsActiveTrueOrderByDisplayOrderAsc();

    Optional<Category> findBySlugAndIsActiveTrue(String slug);

    @Query("""
        SELECT c FROM Category c
        LEFT JOIN FETCH c.subCategories sc
        WHERE c.slug = :slug AND c.isActive = true
    """)
    Optional<Category> findBySlugWithSubCategories(@Param("slug") String slug);
}