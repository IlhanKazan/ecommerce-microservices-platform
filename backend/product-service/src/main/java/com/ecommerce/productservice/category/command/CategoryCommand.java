package com.ecommerce.productservice.category.command;

/**
 * Kategori oluşturma/güncelleme için service katmanı girdisi.
 * Controller Request DTO'ları bunu doldurur. null alanlar update'te "değiştirme" anlamına gelir.
 */
public record CategoryCommand(
        String name,
        String description,
        String imageUrl,
        String icon,
        Integer displayOrder,
        Boolean isActive,
        Long parentId
) {}
