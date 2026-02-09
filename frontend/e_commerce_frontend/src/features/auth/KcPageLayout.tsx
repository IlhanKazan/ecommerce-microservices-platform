import React, { ReactNode } from 'react';
import { Box, Paper, Container } from '@mui/material';
import { ThemeProvider } from '@mui/material/styles';
import customTheme from './theme/customTheme';
import { GetKcContext } from 'keycloakify';

interface KcPageLayoutProps {
    children: ReactNode;
    kcContext: GetKcContext;
    t: (key: string) => string;
}

export const KcPageLayout: React.FC<KcPageLayoutProps> = ({ children }) => {

    return (
        <ThemeProvider theme={customTheme}>
            <Box
                sx={{
                    minHeight: '100vh',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    bgcolor: 'background.default'
                }}
            >
                <Container maxWidth="sm">
                    {children}
                </Container>
            </Box>
        </ThemeProvider>
    );
};