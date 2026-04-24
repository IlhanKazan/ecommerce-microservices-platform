import { createTheme } from '@mui/material/styles';

const customTheme = createTheme({
    palette: {
        primary: {
            main: '#7D5525',          // Brown
            light: '#C8A882',         // Lighter brown (35% lighter)
            dark: '#5C3E1A',          // Darker brown (40% darker)
            contrastText: '#FFFFFF',
        },
        secondary: {
            main: '#1D466E',          // Navy blue
            light: '#4A7BA7',         // Lighter navy
            dark: '#132E48',          // Darker navy
        },
        background: {
            default: '#F5F5F5',       // Light gray (neutral)
            paper: '#FFFFFF',         // White
        },
        text: {
            primary: '#212121',
            secondary: '#757575',
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