import type { PageResponse } from '../types/product';

/**
 * Spring Data `Page` iki farklı JSON formatında serialize edilebiliyor:
 *
 *  - FLAT (legacy / default):     { content, totalElements, totalPages, size, number, last }
 *  - NESTED (PagedModel/VIA_DTO): { content, page: { totalElements, totalPages, size, number } }
 *
 * Projede bazı servisler `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`
 * kullanıyor (search-service, payment-service), bazıları flat dönüyor. Bu yüzden
 * frontend her iki şekli de tolere etmeli — yoksa nested dönen serviste
 * `totalElements`/`totalPages` top-level'da bulunamayıp 0 görünür (sayaç + pagination kırılır).
 *
 * Bu normalizer her iki formatı tek `PageResponse<T>` şekline indirger.
 */
export function normalizePage<T>(raw: unknown): PageResponse<T> {
    const root = (raw ?? {}) as Record<string, unknown>;
    // Nested formatta meta `page` altında; flat formatta kökte.
    const meta = (root.page ?? root) as Record<string, unknown>;

    const num = (v: unknown, fallback = 0): number =>
        typeof v === 'number' ? v : fallback;

    return {
        content: Array.isArray(root.content) ? (root.content as T[]) : [],
        totalElements: num(meta.totalElements),
        totalPages: num(meta.totalPages),
        // Spring flat: size/number/last — nested: size/number (last yok, türetilir)
        pageSize: num(meta.size ?? meta.pageSize),
        pageNumber: num(meta.number ?? meta.pageNumber),
        isLast:
            typeof meta.last === 'boolean'
                ? meta.last
                : typeof meta.isLast === 'boolean'
                  ? meta.isLast
                  : num(meta.number ?? meta.pageNumber) + 1 >= num(meta.totalPages),
    };
}
