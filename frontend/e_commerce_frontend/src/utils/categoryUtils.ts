import type { CategoryResponse } from '../types/product';

/**
 * Seçilen kategorinin kendi ID'si + tüm torun kategori ID'lerini döner.
 * Search payload'daki `categoryIds` alanına bu sonuç geçilir.
 *
 * @example
 * // "Giyim" seçilince Giyim + T-shirt + Kazak + ... gibi alt kategoriler de gelir
 * const ids = collectCategoryIds(5, categoryTree); // [5, 12, 13, 18, ...]
 */
export function collectCategoryIds(
    targetId: number,
    tree: CategoryResponse[]
): number[] {
    const ids: number[] = [];

    function traverse(categories: CategoryResponse[]) {
        for (const cat of categories) {
            if (cat.id === targetId) {
                collectAll(cat, ids);
                return;
            }
            traverse(cat.subCategories);
        }
    }

    function collectAll(cat: CategoryResponse, acc: number[]) {
        acc.push(cat.id);
        cat.subCategories.forEach((sub) => collectAll(sub, acc));
    }

    traverse(tree);
    return ids;
}

/**
 * Ağacı düz liste haline getirir — dropdown render için kullanışlı.
 * level bilgisi indent için kullanılabilir.
 */
export function flattenCategories(
    tree: CategoryResponse[],
    depth = 0
): Array<CategoryResponse & { depth: number }> {
    return tree.flatMap((cat) => [
        { ...cat, depth },
        ...flattenCategories(cat.subCategories, depth + 1),
    ]);
}