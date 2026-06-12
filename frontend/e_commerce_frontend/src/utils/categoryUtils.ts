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
 * Hedef kategoriye giden kök→hedef ID zincirini döner (hedef dahil).
 * Sidebar'da seçili kategorinin ata düğümlerini açık başlatmak için kullanılır.
 *
 * @example
 * findCategoryPath(18, tree); // [5, 12, 18]  (Giyim → Üst Giyim → Kazak)
 */
export function findCategoryPath(
    targetId: number,
    tree: CategoryResponse[]
): number[] {
    function dfs(cats: CategoryResponse[], trail: number[]): number[] | null {
        for (const cat of cats) {
            const next = [...trail, cat.id];
            if (cat.id === targetId) return next;
            const found = dfs(cat.subCategories, next);
            if (found) return found;
        }
        return null;
    }
    return dfs(tree, []) ?? [];
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