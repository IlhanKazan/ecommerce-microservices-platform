import { createTheme } from '@mui/material/styles';

const keycloakDarkTheme = createTheme({
    palette: {
        mode: 'dark',
        primary: {
            main: '#7D5525',
            light: '#B0885E',
            dark: '#4D7D25',
        },
        secondary: {
            main: '#1D466E',
        },
        background: {
            default: '#121212',
            paper: '#1e1e1e',
        },
    },
    typography: {
        fontFamily: [
            '-apple-system',
        ].join(','),
    },
    components: {
        MuiCssBaseline: {
            styleOverrides: {
                body: {
                    overflowY: 'scroll',
                    '&::-webkit-scrollbar-thumb': {
                        backgroundColor: 'rgba(255, 255, 255, 0.3)',
                    },
                },
            },
        },
    },
});

export default keycloakDarkTheme;