package com.ecommerce.productservice.category.service.impl;

import com.ecommerce.common.exception.BusinessException;
import com.ecommerce.common.exception.ResourceNotFoundException;
import com.ecommerce.productservice.category.command.CategoryCommand;
import com.ecommerce.productservice.category.entity.Category;
import com.ecommerce.productservice.category.query.CategoryInfo;
import com.ecommerce.productservice.category.repository.CategoryRepository;
import com.ecommerce.productservice.category.service.CategoryService;
import com.ecommerce.productservice.common.exception.CategoryInUseException;
import com.ecommerce.productservice.common.service.ImageService;
import com.ecommerce.productservice.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ImageService imageService;

    @Override
    @Cacheable(cacheNames = "categories-root")
    public List<CategoryInfo> getRootCategories() {
        log.info("Kök kategoriler DB'den çekiliyor (cache miss)");
        List<Category> roots = categoryRepository
                .findByParentCategoryIsNullAndIsActiveTrueOrderByDisplayOrderAsc();
        return roots.stream().map(this::toInfo).toList();
    }

    @Override
    @Cacheable(cacheNames = "category-by-slug", key = "#slug")
    public CategoryInfo getCategoryBySlug(String slug) {
        log.info("Kategori slug ile çekiliyor: {}", slug);
        Category category = categoryRepository.findBySlugWithSubCategories(slug)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kategori bulunamadı: " + slug, "CATEGORY_NOT_FOUND"));
        return toInfo(category);
    }

    // ── Platform admin (CRUD) ──────────────────────────────────────────────

    @Override
    public List<CategoryInfo> getAllForAdmin() {
        // İnaktifler dahil tüm ağaç
        return categoryRepository.findByParentCategoryIsNullOrderByDisplayOrderAsc()
                .stream().map(this::toAdminInfo).toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"categories-root", "category-by-slug"}, allEntries = true)
    public CategoryInfo create(CategoryCommand command) {
        Category parent = resolveParent(command.parentId());
        String slug = generateUniqueSlug(command.name());

        Category category = Category.builder()
                .name(command.name())
                .slug(slug)
                .description(command.description())
                .imageUrl(command.imageUrl())
                .icon(command.icon())
                .displayOrder(command.displayOrder())
                .isActive(command.isActive() == null ? Boolean.TRUE : command.isActive())
                .parentCategory(parent)
                .level(parent == null ? 0 : parent.getLevel() + 1)
                .fullPath(parent == null ? slug : parent.getFullPath() + "/" + slug)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Kategori oluşturuldu: {} (slug={}, level={})", saved.getName(), saved.getSlug(), saved.getLevel());
        return toAdminInfo(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"categories-root", "category-by-slug"}, allEntries = true)
    public CategoryInfo update(Long id, CategoryCommand command) {
        Category category = getOrThrow(id);

        // slug ve fullPath URL kararlılığı için değişmez (warehouse code gibi). Parent değişimi kapsam dışı.
        if (command.name() != null) category.setName(command.name());
        if (command.description() != null) category.setDescription(command.description());
        if (command.imageUrl() != null) category.setImageUrl(command.imageUrl());
        if (command.icon() != null) category.setIcon(command.icon());
        if (command.displayOrder() != null) category.setDisplayOrder(command.displayOrder());
        if (command.isActive() != null) category.setIsActive(command.isActive());

        Category saved = categoryRepository.save(category);
        log.info("Kategori güncellendi: {} (id={})", saved.getName(), id);
        return toAdminInfo(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"categories-root", "category-by-slug"}, allEntries = true)
    public CategoryInfo setStatus(Long id, boolean active) {
        Category category = getOrThrow(id);
        category.setIsActive(active);
        Category saved = categoryRepository.save(category);
        log.info("Kategori durumu değişti: {} -> active={}", id, active);
        return toAdminInfo(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"categories-root", "category-by-slug"}, allEntries = true)
    public CategoryInfo setImage(Long id, String imageUrl) {
        Category category = getOrThrow(id);
        String oldImageUrl = category.getImageUrl();
        category.setImageUrl(imageUrl);
        Category saved = categoryRepository.save(category);
        // Eski görseli MinIO'dan temizle (best-effort; orphan kalmasın)
        if (oldImageUrl != null && !oldImageUrl.equals(imageUrl)) {
            imageService.deleteImage(oldImageUrl);
        }
        log.info("Kategori görseli güncellendi: {} (id={})", saved.getName(), id);
        return toAdminInfo(saved);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"categories-root", "category-by-slug"}, allEntries = true)
    public void delete(Long id) {
        Category category = getOrThrow(id);
        if (categoryRepository.existsByParentCategoryId(id)) {
            throw new CategoryInUseException(
                    "Alt kategorisi olan bir kategori silinemez. Önce alt kategorileri kaldırın.",
                    "CATEGORY_HAS_CHILDREN");
        }
        if (productRepository.existsByCategoryId(id)) {
            throw new CategoryInUseException(
                    "Bağlı ürünü olan bir kategori silinemez. Ürünleri başka kategoriye taşıyın.",
                    "CATEGORY_HAS_PRODUCTS");
        }
        categoryRepository.delete(category);
        log.info("Kategori silindi: {} (id={})", category.getName(), id);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Category getOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Kategori bulunamadı: " + id, "CATEGORY_NOT_FOUND"));
    }

    private Category resolveParent(Long parentId) {
        if (parentId == null) return null;
        return categoryRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Üst kategori bulunamadı: " + parentId, "PARENT_CATEGORY_NOT_FOUND"));
    }

    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        if (base.isBlank()) {
            throw new BusinessException("Kategori adından geçerli bir slug üretilemedi.", "INVALID_CATEGORY_NAME");
        }
        if (!categoryRepository.existsBySlug(base)) return base;
        // Çakışma: -2, -3, ... ekle
        int suffix = 2;
        while (categoryRepository.existsBySlug(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }

    private String slugify(String input) {
        if (input == null) return "";
        String s = input.toLowerCase(Locale.of("tr"))
                .replace('ı', 'i').replace('ğ', 'g').replace('ü', 'u')
                .replace('ş', 's').replace('ö', 'o').replace('ç', 'c');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^a-z0-9]+", "-")  // alfanümerik olmayanları tireye çevir
             .replaceAll("(^-+|-+$)", "");    // baş/son tireleri kırp
        return s;
    }

    private CategoryInfo toInfo(Category category) {
        List<CategoryInfo> subs = category.getSubCategories().stream()
                .filter(Category::getIsActive)
                .sorted(displayOrderComparator())
                .map(this::toInfo)
                .toList();
        return mapInfo(category, subs);
    }

    // Admin görünümü: inaktif alt kategoriler de dahil
    private CategoryInfo toAdminInfo(Category category) {
        List<CategoryInfo> subs = category.getSubCategories().stream()
                .sorted(displayOrderComparator())
                .map(this::toAdminInfo)
                .toList();
        return mapInfo(category, subs);
    }

    private Comparator<Category> displayOrderComparator() {
        return Comparator.comparing(Category::getDisplayOrder,
                Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private CategoryInfo mapInfo(Category category, List<CategoryInfo> subs) {
        return new CategoryInfo(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getIcon(),
                category.getLevel(),
                category.getFullPath(),
                category.getDisplayOrder(),
                category.getIsActive(),
                category.getParentCategory() != null ? category.getParentCategory().getId() : null,
                subs
        );
    }
}
