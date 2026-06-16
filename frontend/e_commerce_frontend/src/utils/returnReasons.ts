/** Yapılandırılmış iade sebepleri — müşteri seçer, kod backend'e gider, merchant/admin label görür. */
export const RETURN_REASONS: { code: string; label: string }[] = [
    { code: 'SIZE_MISMATCH', label: 'Beden / numara uymadı' },
    { code: 'DEFECTIVE', label: 'Üründe defo / hasar var' },
    { code: 'WRONG_ITEM', label: 'Yanlış ürün geldi' },
    { code: 'NOT_SATISFIED', label: 'Üründen memnun kalmadım' },
    { code: 'NOT_AS_DESCRIBED', label: 'Ürün açıklamayla uyuşmuyor' },
    { code: 'DAMAGED_SHIPPING', label: 'Kargo hasarlı geldi' },
    { code: 'OTHER', label: 'Diğer' },
];

/** İade penceresi (gün) — backend ile aynı (TR cayma hakkı). */
export const RETURN_WINDOW_DAYS = 14;

export const returnReasonLabel = (code: string | null | undefined): string =>
    RETURN_REASONS.find((r) => r.code === code)?.label ?? code ?? '—';

/** Teslim tarihinden itibaren iade hâlâ açılabilir mi? deliveredAt null → grace (true). */
export const isWithinReturnWindow = (deliveredAt: string | null | undefined): boolean => {
    if (!deliveredAt) return true;
    const deadline = new Date(deliveredAt).getTime() + RETURN_WINDOW_DAYS * 24 * 60 * 60 * 1000;
    return Date.now() <= deadline;
};
