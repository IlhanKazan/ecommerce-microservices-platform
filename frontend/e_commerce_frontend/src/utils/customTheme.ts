import { createTheme } from '@mui/material/styles';

const customTheme = createTheme({
    palette: {
        primary: {
            main: '#7D5525',
            light: '#797D25',
            dark: '#4D7D25',
        },
        secondary: {
            main: '#1D466E',
        },
        background: {
            default: '#EBC6A2',
            paper: '#F0DECC',
        },
    },
    typography: {
        fontFamily: [
            '-apple-system',
            'BlinkMacSystemFont',
            '"Segoe UI"',
            'Roboto',
            '"Helvetica Neue"',
            'Arial',
            'sans-serif',
            '"Apple Color Emoji"',
            '"Segoe UI Emoji"',
            '"Segoe UI Symbol"',
        ].join(','),
        h1: {
            fontSize: '2.5rem',
            fontWeight: 700,
        },
        button: {
            textTransform: 'none',
        },
    },
    components: {
        MuiButton: {
            defaultProps: {
                disableElevation: true,
            },
            styleOverrides: {
                root: {
                    borderRadius: 8,
                },
            },
        },
    },
});

export default customTheme;