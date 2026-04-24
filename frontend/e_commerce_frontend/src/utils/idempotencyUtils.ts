/**
 * Idempotency key üretimi.
 *
 * Key yönetimi interceptor'da DEĞİL, mutation hook'larında yapılır.
 * Her mutation instance'ı kendi key'ini useRef ile tutar:
 *   - Aynı işlem retry edilirse aynı key gider → Redis dedupe çalışır
 *   - İşlem başarıya ulaşırsa key yenilenir → sonraki işlem farklı key alır
 *   - Kullanıcı formu kapatıp açarsa component unmount/mount olur → yeni key
 */

/** Kriptografik rastgele UUID v4 */
export function generateIdempotencyKey(): string {
    if (
        typeof globalThis.crypto !== 'undefined' &&
        typeof globalThis.crypto.randomUUID === 'function'
    ) {
        return globalThis.crypto.randomUUID();
    }

    // Eski ortam fallback
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
        const r = (Math.random() * 16) | 0;
        const v = c === 'x' ? r : (r & 0x3) | 0x8;
        return v.toString(16);
    });
}

export const IDEMPOTENCY_KEY_HEADER = 'Idempotency-Key' as const;