import React, {type ReactNode } from 'react';
import { ThemeProvider } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';
import keycloakDarkTheme from '../utils/keycloakDarkTheme';

interface MuiThemeProviderProps {
    children: ReactNode;
}

export const MuiThemeProvider: React.FC<MuiThemeProviderProps> = ({ children }) => {
    return (
        <ThemeProvider theme={keycloakDarkTheme}>
            <CssBaseline />
            {children}
        </ThemeProvider>
    );
};