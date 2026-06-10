/**
 * Tasarım token'ları — tema buradan kurulur, sayfalarda da doğrudan import edilebilir.
 * Canlı e-ticaret paleti: turuncu CTA + koyu lacivert + durum renkleri.
 */

export const tokens = {
    color: {
        // Primary — turuncu (CTA, marka vurgusu)
        primary: {
            lighter: '#FFF1E6',
            light: '#FF9D4D',
            main: '#F27A1A',
            dark: '#D9620A',
            darker: '#A8480A',
            contrastText: '#FFFFFF',
        },
        // Secondary — koyu lacivert (metin, footer, merchant panel)
        secondary: {
            lighter: '#E8EAF0',
            light: '#2E3A5C',
            main: '#1A2238',
            dark: '#11172B',
            contrastText: '#FFFFFF',
        },
        // Sale / hata — kırmızı
        error: {
            lighter: '#FDECEA',
            light: '#EF5350',
            main: '#E53935',
            dark: '#C62828',
            contrastText: '#FFFFFF',
        },
        // Stokta — yeşil
        success: {
            lighter: '#E6F4EC',
            light: '#4CAF73',
            main: '#2E9E5B',
            dark: '#1E7A43',
            contrastText: '#FFFFFF',
        },
        // Son ürünler / uyarı — sarı/amber
        warning: {
            lighter: '#FFF6E5',
            light: '#FFC053',
            main: '#F5A623',
            dark: '#C77F00',
            contrastText: '#1A2238',
        },
        // Bilgi — mavi
        info: {
            lighter: '#E8F2FB',
            light: '#5AA9E6',
            main: '#2680C2',
            dark: '#1B5E92',
            contrastText: '#FFFFFF',
        },
        background: {
            default: '#F3F4F6',
            paper: '#FFFFFF',
        },
        text: {
            primary: '#1A2238',
            secondary: '#5A6478',
            disabled: '#9AA1B1',
        },
        divider: '#E6E8EC',
    },

    radius: {
        sm: 8,
        md: 12,
        lg: 16,
        xl: 24,
        pill: 999,
    },

    // Yumuşak, katmanlı gölge sistemi
    shadow: {
        xs: '0 1px 2px rgba(26, 34, 56, 0.06)',
        sm: '0 2px 8px rgba(26, 34, 56, 0.06)',
        md: '0 6px 20px rgba(26, 34, 56, 0.08)',
        lg: '0 12px 32px rgba(26, 34, 56, 0.12)',
        hover: '0 8px 28px rgba(26, 34, 56, 0.14)',
        primary: '0 6px 18px rgba(242, 122, 26, 0.32)',
    },

    // Marka gradyanları (hero, banner)
    gradient: {
        brand: 'linear-gradient(135deg, #F27A1A 0%, #D9620A 100%)',
        dark: 'linear-gradient(135deg, #1A2238 0%, #2E3A5C 100%)',
        sunset: 'linear-gradient(135deg, #1A2238 0%, #6B3FA0 55%, #F27A1A 140%)',
    },
} as const;

export default tokens;
