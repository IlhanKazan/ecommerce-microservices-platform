import { createTheme } from '@mui/material/styles';
import { tokens } from './themeTokens';

// Palette renklerine `lighter` / `darker` varyantları ekliyoruz (ErrorState vb. kullanıyor)
declare module '@mui/material/styles' {
    interface PaletteColor {
        lighter?: string;
        darker?: string;
    }
    interface SimplePaletteColorOptions {
        lighter?: string;
        darker?: string;
    }
}

const FONT_FAMILY = [
    'Inter',
    '-apple-system',
    'BlinkMacSystemFont',
    '"Segoe UI"',
    'Roboto',
    '"Helvetica Neue"',
    'Arial',
    'sans-serif',
    '"Apple Color Emoji"',
    '"Segoe UI Emoji"',
].join(',');

const customTheme = createTheme({
    palette: {
        primary: tokens.color.primary,
        secondary: tokens.color.secondary,
        error: tokens.color.error,
        success: tokens.color.success,
        warning: tokens.color.warning,
        info: tokens.color.info,
        background: tokens.color.background,
        text: tokens.color.text,
        divider: tokens.color.divider,
    },

    // sx borderRadius çarpanının tabanı — düşük tutuyoruz ki `borderRadius: 2` aşırı yuvarlak olmasın (2 × 8 = 16px)
    shape: {
        borderRadius: tokens.radius.sm,
    },

    typography: {
        fontFamily: FONT_FAMILY,
        h1: { fontSize: '2.75rem', fontWeight: 800, letterSpacing: '-0.02em', lineHeight: 1.1 },
        h2: { fontSize: '2.25rem', fontWeight: 800, letterSpacing: '-0.02em', lineHeight: 1.15 },
        h3: { fontSize: '1.875rem', fontWeight: 700, letterSpacing: '-0.01em', lineHeight: 1.2 },
        h4: { fontSize: '1.5rem', fontWeight: 700, letterSpacing: '-0.01em', lineHeight: 1.25 },
        h5: { fontSize: '1.25rem', fontWeight: 700, letterSpacing: '-0.01em', lineHeight: 1.3 },
        h6: { fontSize: '1.0625rem', fontWeight: 700, lineHeight: 1.35 },
        subtitle1: { fontWeight: 600 },
        subtitle2: { fontWeight: 600 },
        body1: { fontSize: '0.95rem', lineHeight: 1.55 },
        body2: { fontSize: '0.875rem', lineHeight: 1.55 },
        button: { textTransform: 'none', fontWeight: 600, letterSpacing: 0 },
        caption: { fontSize: '0.75rem', lineHeight: 1.4 },
    },

    components: {
        MuiCssBaseline: {
            styleOverrides: {
                body: {
                    WebkitFontSmoothing: 'antialiased',
                    MozOsxFontSmoothing: 'grayscale',
                },
            },
        },

        MuiButton: {
            defaultProps: { disableElevation: true },
            styleOverrides: {
                root: {
                    borderRadius: tokens.radius.sm,
                    fontWeight: 600,
                    paddingInline: 18,
                    transition: 'all 0.2s ease',
                },
                sizeLarge: {
                    paddingBlock: 11,
                    fontSize: '0.95rem',
                },
                containedPrimary: {
                    boxShadow: 'none',
                    '&:hover': {
                        boxShadow: tokens.shadow.primary,
                        transform: 'translateY(-1px)',
                    },
                },
                containedSecondary: {
                    '&:hover': { transform: 'translateY(-1px)' },
                },
                outlined: {
                    borderWidth: 1.5,
                    '&:hover': { borderWidth: 1.5 },
                },
            },
        },

        MuiPaper: {
            styleOverrides: {
                rounded: { borderRadius: tokens.radius.md },
            },
        },

        MuiCard: {
            defaultProps: { elevation: 0 },
            styleOverrides: {
                root: {
                    borderRadius: tokens.radius.md,
                    border: `1px solid ${tokens.color.divider}`,
                    boxShadow: tokens.shadow.xs,
                },
            },
        },

        MuiChip: {
            styleOverrides: {
                root: { fontWeight: 600, borderRadius: tokens.radius.sm },
                sizeSmall: { fontSize: '0.7rem' },
            },
        },

        MuiAppBar: {
            styleOverrides: {
                root: {
                    backgroundColor: tokens.color.background.paper,
                    color: tokens.color.text.primary,
                    boxShadow: tokens.shadow.sm,
                },
            },
        },

        MuiOutlinedInput: {
            styleOverrides: {
                root: {
                    borderRadius: tokens.radius.sm,
                    '&:hover .MuiOutlinedInput-notchedOutline': {
                        borderColor: tokens.color.primary.light,
                    },
                    '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
                        borderWidth: 1.5,
                    },
                },
            },
        },

        MuiTooltip: {
            styleOverrides: {
                tooltip: {
                    backgroundColor: tokens.color.secondary.main,
                    fontSize: '0.75rem',
                    borderRadius: tokens.radius.sm,
                    padding: '6px 10px',
                },
                arrow: { color: tokens.color.secondary.main },
            },
        },

        MuiLink: {
            defaultProps: { underline: 'hover' },
        },

        MuiDialog: {
            styleOverrides: {
                paper: { borderRadius: tokens.radius.lg },
            },
        },
    },
});

export default customTheme;
